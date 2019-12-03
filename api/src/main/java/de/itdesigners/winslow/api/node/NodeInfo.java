package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class NodeInfo {

    private final @Nonnull String        name;
    private final @Nonnull CpuInfo       cpuInfo;
    private final @Nonnull MemInfo       memInfo;
    private final @Nonnull NetInfo       netInfo;
    private final @Nonnull DiskInfo      diskInfo;
    private final @Nonnull List<GpuInfo> gpuInfo;
    private final @Nonnull BuildInfo     buildInfo;

    public NodeInfo(
            @Nonnull String name,
            @Nonnull CpuInfo cpuInfo,
            @Nonnull MemInfo memInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo,
            @Nonnull List<GpuInfo> gpuInfo) {
        this.name      = name;
        this.cpuInfo   = cpuInfo;
        this.memInfo   = memInfo;
        this.netInfo   = netInfo;
        this.diskInfo  = diskInfo;
        this.gpuInfo   = Collections.unmodifiableList(gpuInfo);
        this.buildInfo = new BuildInfo();
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

    @Nonnull
    public List<GpuInfo> getGpuInfo() {
        return gpuInfo;
    }

    @Nonnull
    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
