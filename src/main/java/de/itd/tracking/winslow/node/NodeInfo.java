package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;

public class NodeInfo {
    @Nonnull private final String  name;
    @Nonnull private final CpuInfo cpuInfo;

    public NodeInfo(@Nonnull String name, @Nonnull CpuInfo cpuInfo) {
        this.name    = name;
        this.cpuInfo = cpuInfo;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }
}
