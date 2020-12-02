package de.itdesigners.winslow.fs;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;

public class BindWorkspaceDirectory implements WorkDirectoryConfiguration {

    private final @Nonnull Path path;

    public BindWorkspaceDirectory(@Nonnull Path path) {
        this.path = path;
    }

    @Nonnull
    @Override
    public Path getPath() {
        return path;
    }

    @Nonnull
    @Override
    public Optional<DockerVolumeTargetConfiguration> getDockerVolumeConfiguration(@Nonnull Path path) {
        if (path.startsWith(this.path)) {
            return Optional.of(new DockerVolumeTargetConfiguration("bind", null, path.toString()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{path=" + path + "}#" + hashCode();
    }

}
