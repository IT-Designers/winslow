package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;

public class GpuInfo {

    private final @Nonnull String id;
    private final @Nonnull String vendor;
    private final @Nonnull String name;
    private final          float  computeUtilization;
    private final          float  memoryUtilization;

    public GpuInfo(
            @Nonnull String id,
            @Nonnull String vendor,
            @Nonnull String name,
            float computeUtilization,
            float memoryUtilization) {
        this.id                 = id;
        this.vendor             = vendor;
        this.name               = name;
        this.computeUtilization = computeUtilization;
        this.memoryUtilization  = memoryUtilization;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getVendor() {
        return vendor;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public float getComputeUtilization() {
        return computeUtilization;
    }

    public float getMemoryUtilization() {
        return memoryUtilization;
    }
}
