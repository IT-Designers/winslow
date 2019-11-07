package de.itd.tracking.winslow.resource;

import java.nio.file.Path;
import java.util.Optional;

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
     * @param path Path within the directory to resolve
     * @return The {@link Path} to the workspace for the given job id if it exists
     */
    public Optional<Path> getWorkspace(Path path) {
        return getWorkspacesDirectory().map(p -> p.resolve(path)).filter(p -> p.toFile().exists());
    }

    /**
     * @param path                The path to the directory to create
     * @param failIfAlreadyExists Whether to return false if the directory already exists
     * @return Whether the create operation was successful
     */
    public Optional<Path> createWorkspace(Path path, boolean failIfAlreadyExists) {
        return getWorkspacesDirectory()
                .map(p -> p.resolve(path))
                .filter(p -> p.toFile().mkdirs() || (!failIfAlreadyExists && p.toFile().exists()));
    }

    public Optional<Path> getResourceDirectory() {
        return Optional.of(configuration.resolvePathOfResources(workDirectory)).filter(p -> p.toFile().exists() || p
                .toFile()
                .mkdirs());
    }
}
