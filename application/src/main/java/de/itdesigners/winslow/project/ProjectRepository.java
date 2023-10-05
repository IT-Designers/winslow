package de.itdesigners.winslow.project;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.LockedContainer;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProjectRepository extends BaseRepository {

    private static final Logger LOG         = Logger.getLogger(ProjectRepository.class.getSimpleName());
    public static final  int    UUID_LENGTH = 36;

    private final @Nonnull List<Consumer<Pair<String, Handle<Project>>>> projectChangeListeners
            = Collections.synchronizedList(new ArrayList<>());

    public ProjectRepository(
            LockBus lockBus,
            WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        registerLockBusChangeListener();
    }

    private void registerLockBusChangeListener() {
        this.lockBus.registerEventListener(Event.Command.RELEASE, event -> {
            getProjectIdFromLockEventSubject(Path.of(event.getSubject())).ifPresent(projectId -> {
                var handle = getProject(projectId);
                if (handle.exists()) {
                    var pair = new Pair<>(projectId, handle);
                    this.projectChangeListeners.forEach(listener -> listener.accept(pair));
                }
            });
        });
    }

    public void registerProjectChangeListener(@Nonnull Consumer<Pair<String, Handle<Project>>> changeListener) {
        this.projectChangeListeners.add(changeListener);
    }

    public void removeProjectChangeListener(@Nonnull Consumer<Pair<String, Handle<Project>>> changeListener) {
        this.projectChangeListeners.remove(changeListener);
    }

    public Optional<String> getProjectIdForLockSubject(@Nonnull String lockSubject) {
        var absolute = workDirectoryConfiguration.getPath().resolve(lockSubject);
        var prefix   = getRepositoryDirectory().relativize(absolute).toString();
        var index    = prefix.indexOf('.');

        if (index > 0 && absolute.startsWith(getRepositoryDirectory())) {
            return Optional.of(prefix.substring(0, index));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getProjectsDirectory();
    }

    @Nonnull
    public Optional<Project> createProject(@Nonnull User owner, @Nonnull PipelineDefinition pipeline) {
        return this.createProject(owner, pipeline, project -> {
        });
    }

    @Nonnull
    public Optional<Project> createProject(
            @Nonnull User owner,
            @Nonnull PipelineDefinition pipeline,
            @Nonnull Consumer<Project> customizer) {
        var id   = UUID.randomUUID().toString();
        var path = workDirectoryConfiguration.getProjectsDirectory().resolve(id + FILE_EXTENSION);
        return getProject(path).exclusive().flatMap(storable -> {
            try (storable) {
                // it should not yet exist, otherwise the UUID has clashed o.O
                if (storable.get().isPresent()) {
                    storable.close(); // early close so there won't be locks while recursively trying to find an unused UUID
                    return this.createProject(owner, pipeline, customizer);
                }

                var project = new Project(id, owner, pipeline);
                customizer.accept(project);
                storable.update(project);
                return Optional.of(project);

            } catch (LockException e) {
                LOG.log(Level.SEVERE, "Failed to acquire lock for project with id=" + id, e);
                return Optional.empty();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to persist new project");
                return Optional.empty();
            }
        });

    }

    @Nonnull
    public Handle<Project> getProject(String id) {
        return getProject(getRepositoryDirectory().resolve(Path.of(id).getFileName() + FILE_EXTENSION));
    }

    @Nonnull
    public Stream<Handle<Project>> getProjects() {
        return listAllProjectPaths().map(this::getProject);
    }

    @Nonnull
    public Stream<String> getProjectIds() {
        return listAllProjectPaths().map(path -> {
            var fileName = path.getFileName().toString();
            return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
        });
    }

    @Nonnull
    private Stream<Path> listAllProjectPaths() {
        return listAll(FILE_EXTENSION)
                .filter(p -> p
                        .getFileName()
                        .toString()
                        .length() == UUID_LENGTH + BaseRepository.FILE_EXTENSION.length());
    }

    private Handle<Project> getProject(Path path) {
        return createHandle(path, Project.class);
    }

    public boolean deleteProject(String id) {
        var path = getRepositoryDirectory().resolve(Path.of(id).getFileName() + FILE_EXTENSION);
        return getProject(path).exclusive().map(LockedContainer::deleteOmitExceptions).orElse(false);
    }

    @Nonnull
    private Optional<String> getProjectIdFromLockEventSubject(@Nonnull Path path) {
        var pathDirectory = path.getParent();
        var fileName      = path.getFileName().toString();
        if (getRepositoryDirectory().endsWith(pathDirectory) && fileName.endsWith(FILE_EXTENSION)
                && !fileName.endsWith(PipelineRepository.FILE_SUFFIX)
                && !fileName.endsWith(AuthTokenRepository.FILE_SUFFIX)
        ) {
            return Optional.of(fileName.substring(0, fileName.length() - FILE_EXTENSION.length()));
        } else {
            return Optional.empty();
        }
    }
}
