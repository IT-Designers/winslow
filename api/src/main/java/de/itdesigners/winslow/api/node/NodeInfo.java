package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public record NodeInfo(
        @Nonnull String name,
        long time,
        long uptime,
        @Nonnull CpuInfo cpuInfo,
        @Nonnull MemInfo memInfo,
        @Nonnull NetInfo netInfo,
        @Nonnull DiskInfo diskInfo,
        @Nonnull List<GpuInfo> gpuInfo,
        @Nonnull BuildInfo buildInfo,
        @Nonnull List<AllocInfo> allocInfo) {

    private static final Logger LOG = Logger.getLogger(NodeInfo.class.getSimpleName());

    @ConstructorProperties({
            "name",
            "time",
            "uptime",
            "cpuInfo",
            "memInfo",
            "netInfo",
            "diskInfo",
            "gpuInfo",
            "allocInfo"
    })
    public NodeInfo(
            @Nonnull String name,
            @Nullable Long time,
            @Nullable Long uptime,
            @Nonnull CpuInfo cpuInfo,
            @Nonnull MemInfo memInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo,
            @Nonnull List<GpuInfo> gpuInfo,
            @Nullable List<AllocInfo> allocInfo) {
        this(
                name,
                uptime != null ? uptime : 0,
                time != null ? time : 0,
                cpuInfo,
                memInfo,
                netInfo,
                diskInfo,
                Collections.unmodifiableList(gpuInfo),
                new BuildInfo(),
                allocInfo != null ? Collections.unmodifiableList(allocInfo) : Collections.emptyList()
        );
    }

    @Nonnull
    @Override
    public List<AllocInfo> allocInfo() {
        // debugging spurious NPEs
        if (this.allocInfo == null) {
            LOG.warning("allocInfo is null");
            return Collections.emptyList();
        }
        return this.allocInfo
                .stream()
                .filter(i -> {
                    if (i == null) {
                        LOG.warning("allocInfo has at least one item which is null");
                        return false;
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }
}
