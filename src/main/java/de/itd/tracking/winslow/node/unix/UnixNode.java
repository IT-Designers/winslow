package de.itd.tracking.winslow.node.unix;

import de.itd.tracking.winslow.node.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnixNode implements Node {

    private static final Path   PROC                  = Path.of("/", "proc");
    public static final  String CPU_INFO_MODEL_PREFIX = "model name";
    public static final  String CPU_INFO_SEPARATOR    = ":";

    @Nonnull private final String                              name;
    @Nonnull private       List<UnixCpuInfoParser.CpuTimes>    prevCpuTimes;
    @Nonnull private       List<UnixNetIoParser.InterfaceInfo> prevNetBytes;
    @Nonnull private       List<UnixDiskIoParser.DiskInfo>     prevDiskInfo;

    public UnixNode(@Nonnull String name) throws IOException {
        this.name         = name;
        this.prevCpuTimes = getCpuTimes(resolveStat());
        this.prevNetBytes = getInterfaceInfo(resolveNetDev());
        this.prevDiskInfo = getDiskInfo(resolveDiskstats());
    }

    @Nonnull
    private static Path resolveCpuInfo() {
        return PROC.resolve("cpuinfo");
    }

    @Nonnull
    private static Path resolveStat() {
        return PROC.resolve("stat");
    }

    @Nonnull
    private static Path resolveMemInfo() {
        return PROC.resolve("meminfo");
    }

    @Nonnull
    private static Path resolveNetDev() {
        return PROC.resolve("net").resolve("dev");
    }

    @Nonnull
    private static Path resolveDiskstats() {
        return PROC.resolve("diskstats");
    }

    @Nonnull
    @Override
    public String getName() {
        return this.name;
    }

    @Nonnull
    @Override
    public NodeInfo loadInfo() throws IOException {
        var cpuInfo  = loadCpuInfo();
        var memInfo  = loadMemInfo();
        var netInfo  = loadNetInfo();
        var diskInfo = loadDiskInfo();
        return new NodeInfo(name, cpuInfo, memInfo, netInfo, diskInfo);
    }

    @Nonnull
    private CpuInfo loadCpuInfo() throws IOException {
        var model       = loadCpuModel();
        var utilization = loadCpuUtilization();
        return new CpuInfo(model, utilization);
    }

    @Nonnull
    private static String loadCpuModel() throws IOException {
        try (var lines = Files.lines(resolveCpuInfo())) {
            return lines
                    .filter(line -> line.startsWith(CPU_INFO_MODEL_PREFIX))
                    .map(line -> line.split(CPU_INFO_SEPARATOR, 2)[1].trim())
                    .findFirst()
                    .orElseThrow(() -> new IOException("Invalid cpu info file format"));
        }
    }

    @Nonnull
    private List<Float> loadCpuUtilization() throws IOException {
        var current  = getCpuTimes(resolveStat());
        var previous = this.prevCpuTimes;


        var entries = Math.min(current.size(), previous.size());
        var result  = new ArrayList<Float>(entries);
        for (int i = 0; i < entries; ++i) {
            result.add(current.get(i).getChangeSince(previous.get(i)).getUtilization());
        }

        this.prevCpuTimes = current;
        return result;
    }

    private static List<UnixCpuInfoParser.CpuTimes> getCpuTimes(@Nonnull Path path) throws IOException {
        try (var lineStream = Files.lines(path)) {
            List<String> lines      = lineStream.collect(Collectors.toUnmodifiableList());
            String[]     linesArray = new String[lines.size()];
            return new UnixCpuInfoParser(lines.toArray(linesArray)).getCpuTimes();
        }
    }

    @Nonnull
    private static MemInfo loadMemInfo() throws IOException {
        try (var lines = Files.lines(resolveMemInfo())) {
            return UnixMemInfoParser.parseMemInfo(lines);
        }
    }

    private NetInfo loadNetInfo() throws IOException {
        var current  = getInterfaceInfo(resolveNetDev());
        var previous = this.prevNetBytes;

        var receiving    = 0;
        var transmitting = 0;
        var entries      = Math.min(current.size(), previous.size());
        for (int i = 0; i < entries; ++i) {
            var diff = current.get(i).getChangeSince(previous.get(i));
            receiving += diff.getReceived();
            transmitting += diff.getTransmitted();
        }

        this.prevNetBytes = current;
        return new NetInfo(receiving, transmitting);
    }

    private static List<UnixNetIoParser.InterfaceInfo> getInterfaceInfo(@Nonnull Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return UnixNetIoParser
                    .getNetInfoConsiderOnlyPhysicalInterfaces(lines)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    private DiskInfo loadDiskInfo() throws IOException {
        var current  = getDiskInfo(resolveDiskstats());
        var previous = this.prevDiskInfo;

        var reading = 0;
        var writing = 0;
        var entries = Math.min(current.size(), previous.size());
        for (int i = 0; i < entries; ++i) {
            var diff = current.get(i).getChangeSince(previous.get(i));
            reading += diff.getBytesRead();
            writing += diff.getBytesWritten();
        }

        this.prevDiskInfo = current;
        return new DiskInfo(reading, writing);
    }

    private static List<UnixDiskIoParser.DiskInfo> getDiskInfo(@Nonnull Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return UnixDiskIoParser
                    .getDiskInfoConsiderOnlyPhysicalInterfaces(lines)
                    .collect(Collectors.toUnmodifiableList());
        }
    }
}
