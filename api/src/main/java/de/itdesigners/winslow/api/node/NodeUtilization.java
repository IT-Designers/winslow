package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record NodeUtilization(
        long time,
        long uptime,
        @Nonnull List<Float> cpuUtilization,
        @Nonnull MemInfo memoryInfo,
        @Nonnull NetInfo netInfo,
        @Nonnull DiskInfo diskInfo,
        @Nonnull List<GpuUtilization> gpuUtilization) {

    public static final String CSV_TOP_LEVEL_SEPARATOR = ";";
    public static final String CSV_LEVEL_2_SEPARATOR   = ",";

    @Nonnull
    public static NodeUtilization average(long time, long uptime, @Nonnull List<NodeUtilization> nodes) {
        return new NodeUtilization(
                time,
                uptime,
                transposedFloatAverage(nodes.stream().map(n -> n.cpuUtilization)),
                new MemInfo(
                        nodes.stream().mapToLong(n -> n.memoryInfo.memoryTotal()).max().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.memoryInfo.memoryFree()).average().orElse(0),
                        nodes.stream().mapToLong(n -> n.memoryInfo.systemCache()).max().orElse(0),
                        nodes.stream().mapToLong(n -> n.memoryInfo.swapTotal()).max().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.memoryInfo.swapFree()).average().orElse(0)
                ),
                new NetInfo(
                        (long) nodes.stream().mapToLong(n -> n.netInfo.receiving()).average().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.netInfo.transmitting()).average().orElse(0)
                ),
                new DiskInfo(
                        (long) nodes.stream().mapToLong(n -> n.diskInfo.reading()).average().orElse(0),
                        (long) nodes.stream().mapToLong(n -> n.diskInfo.writing()).average().orElse(0),
                        nodes.stream().mapToLong(n -> n.diskInfo.free()).min().orElse(0),
                        nodes.stream().mapToLong(n -> n.diskInfo.used()).max().orElse(0)
                ),
                transposedStream(nodes.stream().map(n -> n.gpuUtilization))
                        .map(gpuUtils -> new GpuUtilization(
                                (float) gpuUtils
                                        .stream()
                                        .mapToDouble(GpuUtilization::computeUtilization)
                                        .average()
                                        .orElse(0),
                                (float) gpuUtils
                                        .stream()
                                        .mapToDouble(GpuUtilization::memoryUtilization)
                                        .average()
                                        .orElse(0),
                                (long) gpuUtils
                                        .stream()
                                        .mapToDouble(GpuUtilization::memoryUsedMegabytes)
                                        .average()
                                        .orElse(0),
                                gpuUtils.stream().mapToLong(GpuUtilization::memoryTotalMegabytes).max().orElse(0)
                        ))
                        .toList()
        );
    }

    @Nonnull
    private static <T> Stream<List<T>> transposedStream(@Nonnull Stream<List<T>> stream) {
        var raw = stream.toList();
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
        var raw     = stream.toList();
        var result  = new ArrayList<Float>();
        var counter = new ArrayList<Float>();

        // sum transposed
        for (var rawList : raw) {
            if (rawList != null) {
                for (int i = 0; i < rawList.size(); ++i) {
                    if (i >= result.size()) {
                        result.add(0.f);
                        counter.add(0.f);
                    }
                    result.set(i, result.get(i) + rawList.get(i));
                    counter.set(i, counter.get(i) + 1);
                }
            }
        }

        // divide each cell by element count
        for (int n = 0; n < result.size(); ++n) {
            result.set(n, result.get(n) / (float) counter.get(n));
        }

        return result;
    }

    @Nonnull
    public static NodeUtilization from(@Nonnull NodeInfo info) {
        return new NodeUtilization(
                info.time(),
                info.uptime(),
                info.cpuInfo()
                    .utilization()
                    .stream()
                    .toList(),
                info.memInfo(),
                info.netInfo(),
                info.diskInfo(),
                info.gpuInfo()
                    .stream()
                    .map(gpu -> new GpuUtilization(
                            gpu.computeUtilization(),
                            gpu.memoryUtilization(),
                            gpu.memoryUsedMegabytes(),
                            gpu.memoryTotalMegabytes()
                    ))
                    .toList()
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
                        String.valueOf(this.memoryInfo.memoryTotal()),
                        String.valueOf(this.memoryInfo.memoryFree()),
                        String.valueOf(this.memoryInfo.systemCache()),
                        String.valueOf(this.memoryInfo.swapTotal()),
                        String.valueOf(this.memoryInfo.swapFree())
                ),
                String.join(
                        CSV_LEVEL_2_SEPARATOR,
                        String.valueOf(this.netInfo.receiving()),
                        String.valueOf(this.netInfo.transmitting())
                ),
                String.join(
                        CSV_LEVEL_2_SEPARATOR,
                        String.valueOf(this.diskInfo.reading()),
                        String.valueOf(this.diskInfo.writing()),
                        String.valueOf(this.diskInfo.free()),
                        String.valueOf(this.diskInfo.used())
                ),
                this.gpuUtilization
                        .stream()
                        .map(GpuUtilization::computeUtilization)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(GpuUtilization::memoryUtilization)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(GpuUtilization::memoryUsedMegabytes)
                        .map(String::valueOf)
                        .collect(Collectors.joining(CSV_LEVEL_2_SEPARATOR)),
                this.gpuUtilization
                        .stream()
                        .map(GpuUtilization::memoryTotalMegabytes)
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
                        .toList(),
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
}
