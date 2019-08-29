package de.itd.tracking.winslow.resource;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceManager {

    private final Path              workDirectory;
    private final PathConfiguration configuration;

    public ResourceManager(Path workDirectory, PathConfiguration configuration) {
        this.workDirectory = workDirectory;
        this.configuration = configuration;
    }

    public Optional<Path> getWorkspacesDirectory() {
        Path path = configuration.resolvePathOfWorkspaces(workDirectory);
        if (!path.toFile().exists() && !path.toFile().mkdirs()) {
            return Optional.empty();
        } else {
            return Optional.of(path);
        }
    }

    /**
     * @param jobId The id of the job to retrieve the workspace for
     * @return The {@link Path} to the workspace for the given job id if it exists
     */
    public Optional<Path> getWorkspace(UUID jobId) {
        return getWorkspacesDirectory()
                .map(p -> p.resolve(jobId.toString()))
                .filter(p -> p.toFile().exists());
    }

    /**
     * @param jobId The id of the job to create the workspace for
     * @param failIfAlreadyExists Whether to return false if the directory already exists
     * @return Whether the create operation was successful
     */
    public Optional<Path> createWorkspace(UUID jobId, boolean failIfAlreadyExists) {
        return getWorkspacesDirectory()
                .map(p -> p.resolve(jobId.toString()))
                .filter(p -> p.toFile().mkdirs() == failIfAlreadyExists);
    }

    public Optional<Path> getResourceDirectory() {
        return Optional.of(configuration.resolvePathOfResources(workDirectory))
                .filter(p -> p.toFile().exists() || p.toFile().mkdirs());
    }
}
