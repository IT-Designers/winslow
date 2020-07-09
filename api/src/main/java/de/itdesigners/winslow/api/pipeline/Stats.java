package de.itdesigners.winslow.api.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public class Stats {

    public final float cpuUsed;
    public final float cpuMaximum;
    public final long  memoryAllocated;
    public final long  memoryMaximum;

    public Stats(float cpuUsed, float cpuMaximum, long memoryAllocated, long memoryMaximum) {
        this.cpuUsed         = cpuUsed;
        this.cpuMaximum      = cpuMaximum;
        this.memoryAllocated = memoryAllocated;
        this.memoryMaximum   = memoryMaximum;
    }

    @Nonnull
    @CheckReturnValue
    public Stats add(@Nonnull Stats other) {
        return new Stats(
                this.cpuUsed + other.cpuUsed,
                this.cpuMaximum + other.cpuMaximum,
                this.memoryAllocated + other.memoryAllocated,
                this.memoryMaximum + other.memoryMaximum
        );
    }
}
