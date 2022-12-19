package de.itdesigners.winslow.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class DockerImage implements Extension {

    private final @Nonnull  String   image;
    private final @Nonnull  String[] arguments;
    private final @Nullable Long     shmSizeMegabytes;
    private final           boolean  privileged;

    public DockerImage(
            @Nonnull String image,
            @Nonnull String[] arguments,
            @Nullable Long shmSizeMegabytes,
            boolean privileged) {
        this.image            = image;
        this.arguments        = arguments;
        this.shmSizeMegabytes = shmSizeMegabytes;
        this.privileged       = privileged;
    }

    @Nonnull
    @CheckReturnValue
    public String getImage() {
        return image;
    }

    @Nonnull
    @CheckReturnValue
    public String[] getArguments() {
        return arguments;
    }

    @CheckReturnValue
    public Optional<Long> getShmSizeMegabytes() {
        return Optional.ofNullable(shmSizeMegabytes);
    }

    @CheckReturnValue
    public boolean isPrivileged() {
        return privileged;
    }
}
