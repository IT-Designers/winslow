package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record ImageInfo(
        @Nonnull String name,
        @Nonnull String[] args,
        long shmMegabytes) {
}
