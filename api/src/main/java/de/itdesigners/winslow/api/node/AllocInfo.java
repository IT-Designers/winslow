package de.itdesigners.winslow.api.node;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public record AllocInfo(
        @Nonnull String title,
        long cpu,
        long memory,
        long gpu
) {

    @Nonnull
    @CheckReturnValue
    public AllocInfo add(@Nonnull AllocInfo info) {
        return this.add(info, this.title);
    }

    @Nonnull
    @CheckReturnValue
    public AllocInfo add(@Nonnull AllocInfo info, @Nonnull String title) {
        return new AllocInfo(
                title,
                this.cpu + info.cpu,
                this.memory + info.memory,
                this.gpu + info.gpu
        );
    }
}
