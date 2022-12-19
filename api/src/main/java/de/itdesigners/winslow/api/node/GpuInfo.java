package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;

public record GpuInfo(
        @Nonnull String id,
        @Nonnull String vendor,
        @Nonnull String name,
        float computeUtilization,
        float memoryUtilization,
        long memoryUsedMegabytes,
        long memoryTotalMegabytes) {

}
