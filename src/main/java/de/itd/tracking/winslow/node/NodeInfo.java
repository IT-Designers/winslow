package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;

public class NodeInfo {
    @Nonnull private final String   name;
    @Nonnull private final CpuInfo  cpuInfo;
    @Nonnull private final MemInfo  memInfo;
    @Nonnull private final NetInfo  netInfo;
    @Nonnull private final DiskInfo diskInfo;

    public NodeInfo(
            @Nonnull String name,
            @Nonnull CpuInfo cpuInfo,
            @Nonnull MemInfo memInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo) {
        this.name     = name;
        this.cpuInfo  = cpuInfo;
        this.memInfo  = memInfo;
        this.netInfo  = netInfo;
        this.diskInfo = diskInfo;
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

    @Nonnull
    public NetInfo getNetInfo() {
        return netInfo;
    }

    @Nonnull
    public DiskInfo getDiskInfo() {
        return diskInfo;
    }
}
