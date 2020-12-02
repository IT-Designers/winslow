package de.itdesigners.winslow.fs;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;

public class BindWorkspaceDirectory implements WorkDirectoryConfiguration {

    private final @Nonnull Path workDir;
    private final @Nonnull Path hostDir;

    public BindWorkspaceDirectory(@Nonnull Path workingDirectory, @Nonnull Path storageDirectory) {
        this.workDir = workingDirectory;
        this.hostDir = storageDirectory;
    }

    @Nonnull
    @Override
    public Path getPath() {
        return workDir;
    }

    @Nonnull
    @Override
    public Optional<DockerVolumeTargetConfiguration> getDockerVolumeConfiguration(@Nonnull Path path) {
        if (path.startsWith(this.workDir)) {
            var relative = this.workDir.relativize(path);
            return Optional.of(new DockerVolumeTargetConfiguration("bind", null, hostDir.resolve(relative).toString()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{path=" + workDir + "}#" + hashCode();
    }

}
