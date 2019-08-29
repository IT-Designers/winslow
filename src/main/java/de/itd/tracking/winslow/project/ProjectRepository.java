package de.itd.tracking.winslow.project;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.fs.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProjectRepository {

    private static final Logger LOG = Logger.getLogger(ProjectRepository.class.getSimpleName());
    private static final String FILE_SUFFIX = ".toml";

    private final LockBus lockBus;
    private final WorkDirectoryConfiguration workDirectoryConfiguration;

    public ProjectRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        this.lockBus = lockBus;
        this.workDirectoryConfiguration = workDirectoryConfiguration;

        var dir = workDirectoryConfiguration.getProjectsDirectory().toFile();
        if (!dir.isDirectory() || (!dir.exists() && !dir.mkdirs())) {
            throw new IOException("Projects directory is not valid: " + dir);
        }
    }

    public Optional<Project> createProject(Pipeline pipeline, User owner) {
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

    public Stream<Project> listProjectsUnsafe() {
        try {
            return Files
                    .list(workDirectoryConfiguration.getProjectsDirectory())
                    .flatMap(path -> {
                        try (InputStream inputStream = new FileInputStream(path.toFile())) {
                            return Stream.of(new Toml().read(inputStream).to(Project.class));
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, "Failed to load project", e);
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list projects", e);
            return Stream.empty();
        }
    }

    public Stream<Project> listProjects() {
        try {
            return Files
                    .list(workDirectoryConfiguration.getProjectsDirectory())
                    .flatMap(path -> {
                        var subject = workDirectoryConfiguration.getPath().relativize(path.toAbsolutePath()).toString();
                        try (Lock lock = new Lock(lockBus, subject)) {
                            try (InputStream inputStream = new LockedInputStream(path.toFile(), lock)) {
                                return Stream.of(new Toml().read(inputStream).to(Project.class));
                            }
                        } catch (LockException | IOException e) {
                            LOG.log(Level.SEVERE, "Failed to load project", e);
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list projects", e);
            return Stream.empty();
        }
    }
}
