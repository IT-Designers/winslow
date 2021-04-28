package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeUtilization {

    public static final String CSV_TOP_LEVEL_SEPARATOR = ";";
    public static final String CSV_LEVEL_2_SEPARATOR   = ",";

    public final          long        time;
    public final          long        uptime;
    public final @Nonnull List<Float> cpuUtilization;
    public final @Nonnull MemInfo     memoryInfo;
    public final @Nonnull NetInfo     netInfo;
    public final @Nonnull DiskInfo    diskInfo;
    public final @Nonnull List<Float> gpuComputeUtilization;
    public final @Nonnull List<Float> gpuMemoryUtilization;

    private NodeUtilization(
            long time,
            long uptime,
            @Nonnull List<Float> cpuUtilization,
            @Nonnull MemInfo memoryInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo,
            @Nonnull List<Float> gpuComputeUtilization,
            @Nonnull List<Float> gpuMemoryUtilization) {
        this.time                  = time;
        this.uptime                = uptime;
        this.cpuUtilization        = cpuUtilization;
        this.memoryInfo            = memoryInfo;
        this.netInfo               = netInfo;
        this.diskInfo              = diskInfo;
        this.gpuComputeUtilization = gpuComputeUtilization;
        this.gpuMemoryUtilization  = gpuMemoryUtilization;
    }

    @Nonnull
    public static NodeUtilization from(@Nonnull NodeInfo info) {
        return new NodeUtilization(
                info.getTime(),
                info.getUptime(),
                info.getCpuInfo().getUtilization().stream().collect(Collectors.toUnmodifiableList()),
                info.getMemInfo(),
                info.getNetInfo(),
                info.getDiskInfo(),
                info
                        .getGpuInfo()
                        .stream()
                        .sorted(Comparator.comparing(GpuInfo::getId))
                        .map(GpuInfo::getComputeUtilization)
                        .collect(Collectors.toUnmodifiableList()),
                info
                        .getGpuInfo()
                        .stream()
                        .sorted(Comparator.comparing(GpuInfo::getId))
                        .map(GpuInfo::getMemoryUtilization)
                        .collect(Collectors.toUnmodifiableList())
        );
    }

    @Nonnull
    public String toCsvLine() {
        return String.join(
                CSV_TOP_LEVEL_SEPARATOR,
                String.valueOf(this.time),
                String.valueOf(this.uptime),
                this.cpuUtilization.stream().map(String::valueOf).collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                String.join(
                        CSV_LEVEL_2_SEPARATOR,
                        String.valueOf(this.memoryInfo.getMemoryTotal()),
                        String.valueOf(this.memoryInfo.getMemoryFree()),
                        String.valueOf(this.memoryInfo.getSystemCache()),
                        String.valueOf(this.memoryInfo.getSwapTotal()),
                        String.valueOf(this.memoryInfo.getSwapFree())
                ),
                String.join(
                        CSV_LEVEL_2_SEPARATOR,
                        String.valueOf(this.netInfo.getReceiving()),
                        String.valueOf(this.netInfo.getTransmitting())
                        ),
                String.join(
                        CSV_LEVEL_2_SEPARATOR,
                        String.valueOf(this.diskInfo.getReading()),
                        String.valueOf(this.diskInfo.getWriting()),
                        String.valueOf(this.diskInfo.getFree()),
                        String.valueOf(this.diskInfo.getUsed())
                ),
                this.gpuComputeUtilization
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuMemoryUtilization
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR))
        );
    }

    @Nonnull
    public static Optional<NodeUtilization> fromCsvLineNoThrows(@Nonnull String line) {
        try {
            return Optional.of(fromCsvLine(line));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Nonnull
    public static NodeUtilization fromCsvLine(@Nonnull String line) {
        var split = Arrays.stream(line.split(CSV_TOP_LEVEL_SEPARATOR)).iterator();
        return new NodeUtilization(
                Long.parseLong(split.next()),
                Long.parseLong(split.next()),
                Arrays
                        .stream(split.next().split(CSV_LEVEL_2_SEPARATOR))
                        .map(Float::parseFloat)
                        .collect(Collectors.toUnmodifiableList()),
                Optional
                        .ofNullable(split.next())
                        .map(memoryInfo -> {
                            var memSplit = Arrays.stream(memoryInfo.split(CSV_LEVEL_2_SEPARATOR)).iterator();
                            return new MemInfo(
                                    Long.parseLong(memSplit.next()),
                                    Long.parseLong(memSplit.next()),
                                    Long.parseLong(memSplit.next()),
                                    Long.parseLong(memSplit.next()),
                                    Long.parseLong(memSplit.next())
                            );
                        })
                        .orElseThrow(),
                Optional
                        .ofNullable(split.next())
                        .map(netInfo -> {
                            var netSplit = Arrays.stream(netInfo.split(CSV_LEVEL_2_SEPARATOR)).iterator();
                            return new NetInfo(
                                    Long.parseLong(netSplit.next()),
                                    Long.parseLong(netSplit.next())
                            );
                        })
                        .orElseThrow(),
                Optional
                        .ofNullable(split.next())
                        .map(diskInfo -> {
                            var diskSplit = Arrays.stream(diskInfo.split(CSV_LEVEL_2_SEPARATOR)).iterator();
                            return new DiskInfo(
                                    Long.parseLong(diskSplit.next()),
                                    Long.parseLong(diskSplit.next()),
                                    Long.parseLong(diskSplit.next()),
                                    Long.parseLong(diskSplit.next())
                            );
                        })
                        .orElseThrow(),
                Stream.ofNullable(split.next())
                      .flatMap(gpuUtil -> Arrays.stream(gpuUtil.split(CSV_LEVEL_2_SEPARATOR)))
                      .map(Float::parseFloat)
                      .collect(Collectors.toUnmodifiableList()),
                Stream.ofNullable(split.next())
                      .flatMap(gpuUtil -> Arrays.stream(gpuUtil.split(CSV_LEVEL_2_SEPARATOR)))
                      .map(Float::parseFloat)
                      .collect(Collectors.toUnmodifiableList())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeUtilization that = (NodeUtilization) o;
        return time == that.time && uptime == that.uptime && cpuUtilization.equals(that.cpuUtilization) && memoryInfo.equals(
                that.memoryInfo) && netInfo.equals(that.netInfo) && diskInfo.equals(that.diskInfo) && gpuComputeUtilization
                .equals(
                        that.gpuComputeUtilization) && gpuMemoryUtilization.equals(that.gpuMemoryUtilization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                time,
                uptime,
                cpuUtilization,
                memoryInfo,
                netInfo,
                diskInfo,
                gpuComputeUtilization,
                gpuMemoryUtilization
        );
    }

    @Override
    public String toString() {
        return "NodeUtilization{" +
                "time=" + time +
                ", uptime=" + uptime +
                ", cpuUtilization=" + cpuUtilization +
                ", memoryInfo=" + memoryInfo +
                ", netInfo=" + netInfo +
                ", diskInfo=" + diskInfo +
                ", gpuComputeUtilization=" + gpuComputeUtilization +
                ", gpuMemoryUtilization=" + gpuMemoryUtilization +
                "}@" + hashCode();
    }
}
