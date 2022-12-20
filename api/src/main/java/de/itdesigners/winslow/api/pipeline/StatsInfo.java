package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record StatsInfo(
        @Nonnull String stageId,
        @Nonnull String nodeName,
        float cpuUsed,
        float cpuMaximum,
        long memoryAllocated,
        long memoryMaximum
) {

}
