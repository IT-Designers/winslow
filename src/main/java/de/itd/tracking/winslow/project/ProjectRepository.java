package de.itd.tracking.winslow.project;

import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.fs.*;

import java.io.IOException;
import java.io.OutputStream;
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

        var dir = workDirectoryConfiguration.getProjectsDirectory().toFile();
        if (!dir.isDirectory() || (!dir.exists() && !dir.mkdirs())) {
            throw new IOException("Projects directory is not valid: " + dir);
        }
    }

    public Optional<Project> createProject(Pipeline pipeline, User owner) {
        return this.createProject(pipeline, owner, project -> {});
    }

    public Optional<Project> createProject(Pipeline pipeline, User owner, Consumer<Project> customizer) {
        var id = UUID.randomUUID().toString();
        var path = workDirectoryConfiguration.getProjectsDirectory().resolve(id + FILE_SUFFIX);
        var subject = workDirectoryConfiguration.getPath().relativize(path).toString();
        try (Lock lock = new Lock(lockBus, subject)) {
            if (path.toFile().exists()) {
                lock.release();
                return this.createProject(pipeline, owner);
            }
            try (OutputStream os = new LockedOutputStream(path.toFile(), lock)) {
                var project = new Project(id, pipeline, owner.getName());
                customizer.accept(project);
                new TomlWriter().write(project, os);
                return Optional.of(project);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to persist new project");
                return Optional.empty();
            }
        } catch (LockException e) {
            LOG.log(Level.SEVERE, "Failed to acquire lock for project with id="+id, e);
            return Optional.empty();
        }
    }

    public Stream<Project> getProjectsUnsafe() {
        return getAllInDirectoryUnsafe(
                workDirectoryConfiguration.getProjectsDirectory(),
                Project.class
        );
    }

    public Stream<Project> getProjects() {
        return getAllInDirectory(
                workDirectoryConfiguration.getProjectsDirectory(),
                Project.class
        );
    }
}
