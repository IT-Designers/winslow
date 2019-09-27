package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;

public class NodeInfo {
    @Nonnull private final String  name;
    @Nonnull private final CpuInfo cpuInfo;
    @Nonnull private final MemInfo memInfo;

    public NodeInfo(@Nonnull String name, @Nonnull CpuInfo cpuInfo, @Nonnull MemInfo memInfo) {
        this.name    = name;
        this.cpuInfo = cpuInfo;
        this.memInfo = memInfo;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }

    @Nonnull
    public MemInfo getMemInfo() {
        return memInfo;
    }
}
