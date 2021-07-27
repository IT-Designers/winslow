package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;

public class Stats {

    /**
     * Nullable because of backwards compatibility
     */
    public final @Nullable String stageId;
    /**
     * Nullable because of backwards compatibility
     */
    public final @Nullable String runningOnNode;
    public final           float  cpuUsed;
    public final           float  cpuMaximum;
    public final           long   memoryAllocated;
    public final           long   memoryMaximum;

    public Stats(
            String stageId,
            String runningOnNode,
            float cpuUsed,
            float cpuMaximum,
            long memoryAllocated,
            long memoryMaximum) {
        this.stageId         = stageId;
        this.runningOnNode   = runningOnNode;
        this.cpuUsed         = cpuUsed;
        this.cpuMaximum      = cpuMaximum;
        this.memoryAllocated = memoryAllocated;
        this.memoryMaximum   = memoryMaximum;
    }
}
