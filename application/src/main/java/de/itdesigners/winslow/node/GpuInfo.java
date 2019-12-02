package de.itdesigners.winslow.node;

import javax.annotation.Nonnull;

public class GpuInfo {

    @Nonnull private final String vendor;
    @Nonnull private final String name;

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
