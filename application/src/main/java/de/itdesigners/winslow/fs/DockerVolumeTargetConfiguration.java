package de.itdesigners.winslow.fs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class DockerVolumeTargetConfiguration {

    private final @Nonnull  String type;
    private final @Nullable String options;
    private final @Nonnull  String targetPath;

    public DockerVolumeTargetConfiguration(
            @Nonnull String type,
            @Nullable String options,
            @Nonnull String targetPath) {
        this.type       = type;
        this.options    = options;
        this.targetPath = targetPath;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public Optional<String> getOptions() {
        return Optional.ofNullable(options);
    }

    @Nonnull
    public String getTargetPath() {
        return targetPath;
    }
}
