package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.BaseRepository;
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

    private static final Logger LOG = Logger.getLogger(ProjectRepository.class.getSimpleName());
    private static final String FILE_SUFFIX = ".toml";

    public ProjectRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getProjectsDirectory();
    }

    public Optional<Project> createProject(PipelineDefinition pipelineDefinition, User owner) {
        return this.createProject(pipelineDefinition, owner, project -> {});
    }

    // TODO ProjectBuilder
    public Optional<Project> createProject(PipelineDefinition pipelineDefinition, User owner, Consumer<Project> customizer) {
        var id = UUID.randomUUID().toString();
        var path = workDirectoryConfiguration.getProjectsDirectory().resolve(id + FILE_SUFFIX);
        return getProject(path).exclusive().flatMap(storable -> {
            try (storable) {
                // it should not yet exist, otherwise the UUID has clashed o.O
                if (storable.get().isPresent()) {
                    storable.close(); // early close so there wont be locks while recursively trying to find an unused UUID
                    return this.createProject(pipelineDefinition, owner);
                }

                var project = new Project(id, pipelineDefinition, owner.getName());
                customizer.accept(project);
                storable.update(project);
                return Optional.of(project);

            } catch (LockException e) {
                LOG.log(Level.SEVERE, "Failed to acquire lock for project with id="+id, e);
                return Optional.empty();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to persist new project");
                return Optional.empty();
            }
        });

    }

    @Nonnull
    public Handle<Project> getProject(String id) {
        return getProject(getRepositoryDirectory().resolve(Path.of(id).getFileName() + FILE_SUFFIX));
    }

    @Nonnull
    public Stream<Handle<Project>> getProjects() {
        return listAll().map(this::getProject);
    }

    private Handle<Project> getProject(Path path) {
        return createHandle(path, Project.class);
    }
}
