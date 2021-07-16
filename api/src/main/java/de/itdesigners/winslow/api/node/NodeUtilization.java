package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeUtilization {

    public static final String CSV_TOP_LEVEL_SEPARATOR = ";";
    public static final String CSV_LEVEL_2_SEPARATOR   = ",";

    public final          long                 time;
    public final          long                 uptime;
    public final @Nonnull List<Float>          cpuUtilization;
    public final @Nonnull MemInfo              memoryInfo;
    public final @Nonnull NetInfo              netInfo;
    public final @Nonnull DiskInfo             diskInfo;
    public final @Nonnull List<GpuUtilization> gpuUtilization;

    private NodeUtilization(
            long time,
            long uptime,
            @Nonnull List<Float> cpuUtilization,
            @Nonnull MemInfo memoryInfo,
            @Nonnull NetInfo netInfo,
            @Nonnull DiskInfo diskInfo,
            @Nonnull List<GpuUtilization> gpuUtilization
    ) {
        this.time           = time;
        this.uptime         = uptime;
        this.cpuUtilization = cpuUtilization;
        this.memoryInfo     = memoryInfo;
        this.netInfo        = netInfo;
        this.diskInfo       = diskInfo;
        this.gpuUtilization = gpuUtilization;
    }

    @Nonnull
    public static NodeUtilization average(long time, long uptime, @Nonnull List<NodeUtilization> nodes) {
        return new NodeUtilization(
                time,
                uptime,
                transposedFloatAverage(nodes.stream().map(n -> n.cpuUtilization)),
                new MemInfo(
                        nodes.stream().mapToLong(n -> n.memoryInfo.getMemoryTotal()).max().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.memoryInfo.getMemoryFree()).average().orElse(0),
                        nodes.stream().mapToLong(n -> n.memoryInfo.getSystemCache()).max().orElse(0),
                        nodes.stream().mapToLong(n -> n.memoryInfo.getSwapTotal()).max().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.memoryInfo.getSwapFree()).average().orElse(0)
                ),
                new NetInfo(
                        (long) nodes.stream().mapToLong(n -> n.netInfo.getReceiving()).average().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.netInfo.getTransmitting()).average().orElse(0)
                ),
                new DiskInfo(
                        (long) nodes.stream().mapToLong(n -> n.diskInfo.getReading()).average().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.diskInfo.getWriting()).average().orElse(0),
                        nodes.stream().mapToLong(n -> n.diskInfo.getFree()).min().orElse(0),
                        nodes.stream().mapToLong(n -> n.diskInfo.getUsed()).max().orElse(0)
                ),
                transposedStream(nodes.stream().map(n -> n.gpuUtilization))
                        .map(gpuUtils -> new GpuUtilization(
                                (float) gpuUtils
                                        .stream()
                                        .mapToDouble(g -> g.computeUtilization)
                                        .average()
                                        .orElse(0),
                                (float) gpuUtils.stream().mapToDouble(g -> g.memoryUtilization).average().orElse(0),
                                (long) gpuUtils
                                        .stream()
                                        .mapToDouble(g -> g.memoryUsedMegabytes)
                                        .average()
                                        .orElse(0),
                                gpuUtils.stream().mapToLong(g -> g.memoryTotalMegabytes).max().orElse(0)
                        ))
                        .collect(Collectors.toUnmodifiableList())
        );
    }

    @Nonnull
    private static <T> Stream<List<T>> transposedStream(@Nonnull Stream<List<T>> stream) {
        var raw = stream.collect(Collectors.toUnmodifiableList());
        if (raw.isEmpty()) {
            return Stream.empty();
        }

        var transposed = new ArrayList<List<T>>(raw.get(0).size());

        for (var rawList : raw) {
            for (int i = 0; i < rawList.size(); ++i) {
                if (i >= transposed.size()) {
                    transposed.add(new ArrayList<>());
                }
                transposed.get(i).add(rawList.get(i));
            }
        }

        return transposed.stream();
    }

    private static List<Float> transposedFloatAverage(Stream<List<Float>> stream) {
        var raw    = stream.collect(Collectors.toUnmodifiableList());
        var result = raw.isEmpty() ? new ArrayList<Float>() : new ArrayList<>(raw.get(0));

        // sum transposed
        for (int i = 1; i < raw.size(); ++i) {
            for (int n = 0; n < result.size(); ++n) {
                result.set(n, result.get(n) + raw.get(i).get(n));
            }
        }

        // divide each cell by element count
        for (int n = 0; n < result.size(); ++n) {
            result.set(n, result.get(n) / (float) raw.size());
        }

        return result;
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
                info.getGpuInfo()
                    .stream()
                    .map(gpu -> new GpuUtilization(
                            gpu.getComputeUtilization(),
                            gpu.getMemoryUtilization(),
                            gpu.getMemoryUsedMegabytes(),
                            gpu.getMemoryTotalMegabytes()
                    ))
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
                this.gpuUtilization
                        .stream()
                        .map(util -> util.computeUtilization)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(util -> util.memoryUtilization)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(util -> util.memoryUsedMegabytes)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(util -> util.memoryTotalMegabytes)
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
                gpuUtilizationFromCsvSplit(split)
        );
    }

    @Nonnull
    private static List<GpuUtilization> gpuUtilizationFromCsvSplit(@Nonnull Iterator<String> split) {
        if (!split.hasNext()) {
            return Collections.emptyList();
        }

        var gpuUtilization = split.next();
        var memUtilization = split.next();
        var memUsed        = split.hasNext() ? split.next() : null;
        var memTotal       = split.hasNext() ? split.next() : null;

        var gpuUtilSplit  = gpuUtilization.split(CSV_LEVEL_2_SEPARATOR);
        var memUtilSplit  = memUtilization.split(CSV_LEVEL_2_SEPARATOR);
        var memUsedSplit  = memUsed != null ? memUsed.split(CSV_LEVEL_2_SEPARATOR) : null;
        var memTotalSplit = memTotal != null ? memTotal.split(CSV_LEVEL_2_SEPARATOR) : null;

        var list = new ArrayList<GpuUtilization>(gpuUtilSplit.length);

        for (int i = 0; i < gpuUtilSplit.length; ++i) {
            list.add(new GpuUtilization(
                    Float.parseFloat(gpuUtilSplit[i]),
                    Float.parseFloat(memUtilSplit[i]),
                    memUsedSplit != null ? Long.parseLong(memUsedSplit[i]) : 0,
                    memTotalSplit != null ? Long.parseLong(memTotalSplit[i]) : 0
            ));
        }

        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeUtilization that = (NodeUtilization) o;
        return time == that.time && uptime == that.uptime && cpuUtilization.equals(that.cpuUtilization) && memoryInfo.equals(
                that.memoryInfo) && netInfo.equals(that.netInfo) && diskInfo.equals(that.diskInfo)
                && gpuUtilization.equals(that.gpuUtilization);
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
                gpuUtilization
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
                ", gpuUtilization=" + gpuUtilization +
                "}@" + hashCode();
    }
}
