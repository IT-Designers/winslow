package de.itdesigners.winslow.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public class DockerNfsVolume {

    private final @Nonnull String name;
    private final @Nonnull String targetPath;
    private final @Nonnull String serverPath;
    private final @Nonnull String options;
    private final boolean readonly;

    public DockerNfsVolume(
            @Nonnull String name,
            @Nonnull String targetPath,
            @Nonnull String serverPath,
            @Nonnull String options,
            boolean readonly) {
        this.name       = name;
        this.targetPath = targetPath;
        this.serverPath = serverPath;
        this.options    = options;
        this.readonly   = readonly;
    }

    @Nonnull
    @CheckReturnValue
    public String getName() {
        return name;
    }

    @Nonnull
    @CheckReturnValue
    public String getTargetPath() {
        return targetPath;
    }

    @Nonnull
    @CheckReturnValue
    public String getServerPath() {
        return serverPath;
    }

    @Nonnull
    @CheckReturnValue
    public String getOptions() {
        return options;
    }

    @CheckReturnValue
    public boolean isReadonly() {
        return readonly;
    }
}
