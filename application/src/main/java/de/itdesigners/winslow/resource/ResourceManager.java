package de.itdesigners.winslow.resource;

import javax.annotation.Nonnull;
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
     * @return The path to the workspace for the given job id if it exists
     */
    @Nonnull
    public Optional<Path> getWorkspace(@Nonnull Path path) {
        return getWorkspacesDirectory()
                .flatMap(dir -> saveResolve(dir, path))
                .filter(p -> p.toFile().exists());
    }

    /**
     * @param path                The path to the directory to create
     * @param failIfAlreadyExists Whether to return false if the directory already exists
     * @return Whether the create operation was successful
     */
    public Optional<Path> createWorkspace(Path path, boolean failIfAlreadyExists) {
        return getWorkspacesDirectory()
                .flatMap(dir -> saveResolve(dir, path))
                .filter(p -> {
                    if (p.toFile().mkdirs()) {
                        return true;
                    } else if (failIfAlreadyExists) {
                        return false;
                    } else {
                        return p.toFile().exists() && p.toFile().isDirectory();
                    }
                });
    }

    @Nonnull
    public Optional<Path> getResourceDirectory() {
        return Optional
                .of(configuration.resolvePathOfResources(workDirectory))
                .filter(p -> p.toFile().exists() || p.toFile().mkdirs());
    }


    public static Optional<Path> saveResolve(@Nonnull Path base, @Nonnull Path path) {
        var resolved = base.resolve(path.normalize()).normalize();
        if (resolved.startsWith(base)) {
            return Optional.of(resolved);
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public Optional<Path> getSubResourcesPath(@Nonnull Path path) {
        return getSubPath(configuration.getRelativePathOfResources(), path);
    }

    @Nonnull
    public Optional<Path> getSubWorkspacePath(@Nonnull Path path) {
        return getSubPath(configuration.getRelativePathOfWorkspaces(), path);
    }

    @Nonnull
    private Optional<Path> getSubPath(@Nonnull Path base, @Nonnull Path path) {
        try {
            path = path.normalize();
            if (path.isAbsolute()) {
                path = workDirectory.relativize(path).normalize();
            }
            if (path.startsWith(base)) {
                return Optional.of(base.relativize(path));
            } else {
                return Optional.empty();
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
