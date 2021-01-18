package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

public class NodeInfo {

    private final @Nonnull String          name;
    private final          long            time;
    private final @Nonnull CpuInfo         cpuInfo;
    private final @Nonnull MemInfo         memInfo;
    private final @Nonnull NetInfo         netInfo;
    private final @Nonnull DiskInfo        diskInfo;
    private final @Nonnull List<GpuInfo>   gpuInfo;
    private final @Nonnull BuildInfo       buildInfo;
    private final @Nonnull List<AllocInfo> allocInfo;

    @ConstructorProperties({
            "name",
            "time",
            "cpuInfo",
            "memInfo",
            "netInfo",
            "diskInfo",
            "gpuInfo",
            "buildInfo"
    })
    public NodeInfo(
            @Nonnull String name,
            @Nullable Long time,
            @Nonnull CpuInfo cpuInfo,
            @Nonnull MemInfo memInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo,
            @Nonnull List<GpuInfo> gpuInfo,
            @Nullable List<AllocInfo> allocInfo) {
        this.name      = name;
        this.time      = time != null ? time : 0;
        this.cpuInfo   = cpuInfo;
        this.memInfo   = memInfo;
        this.netInfo   = netInfo;
        this.diskInfo  = diskInfo;
        this.gpuInfo   = Collections.unmodifiableList(gpuInfo);
        this.buildInfo = new BuildInfo();
        this.allocInfo = allocInfo != null ? Collections.unmodifiableList(allocInfo) : Collections.emptyList();
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
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

    @Nonnull
    public List<AllocInfo> getAllocInfo() {
        return allocInfo;
    }
}
