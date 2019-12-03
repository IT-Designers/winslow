package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;

public class GpuInfo {

    private final @Nonnull String vendor;
    private final @Nonnull String name;

    public GpuInfo(@Nonnull String vendor, @Nonnull String name) {
        this.vendor = vendor;
        this.name   = name;
    }

    @Nonnull
    public String getVendor() {
        return vendor;
    }

    @Nonnull
    public String getName() {
        return name;
    }
}
