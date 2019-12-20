package de.itdesigners.winslow.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public class DockerImage implements Extension {

    private final @Nonnull String   image;
    private final @Nonnull String[] arguments;

    public DockerImage(@Nonnull String image, @Nonnull String[] arguments) {
        this.image     = image;
        this.arguments = arguments;
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
}
