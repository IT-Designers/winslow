package de.itdesigners.winslow.api.settings;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record ResourceLimitation(
        @Nullable Long cpu,
        @Nullable Long mem,
        @Nullable Long gpu
) {
    @Nonnull
    @CheckReturnValue
    public ResourceLimitation min(@Nonnull ResourceLimitation other) {
        return new ResourceLimitation(
                cpu == null ? other.cpu : (Long)Math.min(cpu, other.cpu != null ? other.cpu : Long.MAX_VALUE),
                mem == null ? other.mem : (Long)Math.min(mem, other.mem != null ? other.mem : Long.MAX_VALUE),
                gpu == null ? other.gpu : (Long)Math.min(gpu, other.gpu != null ? other.gpu : Long.MAX_VALUE)
        );
    }
}
