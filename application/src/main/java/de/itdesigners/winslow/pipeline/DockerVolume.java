package de.itdesigners.winslow.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public class DockerVolume {

    private final @Nonnull String  name;
    private final @Nonnull String  type;
    private final @Nonnull String  containerPath;
    private final @Nonnull String  hostPath;
    private final @Nonnull String  options;
    private final          boolean readonly;

    public DockerVolume(
            @Nonnull String name,
            @Nonnull String type,
            @Nonnull String containerPath,
            @Nonnull String hostPath,
            @Nonnull String options,
            boolean readonly) {
        this.name          = name;
        this.type          = type;
        this.containerPath = containerPath;
        this.hostPath      = hostPath;
        this.options       = options;
        this.readonly      = readonly;
    }

    @Nonnull
    @CheckReturnValue
    public String getName() {
        return name;
    }

    @Nonnull
    @CheckReturnValue
    public String getType() {
        return type;
    }

    @Nonnull
    @CheckReturnValue
    public String getContainerPath() {
        return containerPath;
    }

    @Nonnull
    @CheckReturnValue
    public String getHostPath() {
        return hostPath;
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
