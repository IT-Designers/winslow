package de.itdesigners.winslow.api.node;

public record GpuUtilization(
        float computeUtilization,
        float memoryUtilization,
        long memoryUsedMegabytes,
        long memoryTotalMegabytes) {

}
