package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.LockedContainer;
import de.itd.tracking.winslow.PipelineRepository;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProjectRepository extends BaseRepository {

    private static final Logger LOG         = Logger.getLogger(ProjectRepository.class.getSimpleName());

    public ProjectRepository(
            LockBus lockBus,
            WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    public Optional<String> getProjectIdForLockSubject(@Nonnull String lockSubject) {
        var absolute = workDirectoryConfiguration.getPath().resolve(lockSubject);
        var prefix = getRepositoryDirectory().relativize(absolute).toString();
        var index = prefix.indexOf('.');

        if (index > 0) {
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
                    storable.close(); // early close so there wont be locks while recursively trying to find an unused UUID
                    return this.createProject(owner, pipeline, customizer);
                }

                var project = new Project(id, owner.getName(), pipeline);
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
        return listAll(FILE_EXTENSION)
                .filter(p -> !p.getFileName().toString().endsWith(PipelineRepository.FILE_SUFFIX))
                .map(this::getProject);
    }

    private Handle<Project> getProject(Path path) {
        return createHandle(path, Project.class);
    }

    public boolean deleteProject(String id) {
        var path = getRepositoryDirectory().resolve(Path.of(id).getFileName() + FILE_EXTENSION);
        return getProject(path).exclusive().map(LockedContainer::deleteOmitExceptions).orElse(false);
    }
}
