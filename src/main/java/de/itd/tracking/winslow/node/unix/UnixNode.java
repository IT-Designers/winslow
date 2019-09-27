package de.itd.tracking.winslow.node.unix;

import de.itd.tracking.winslow.node.CpuInfo;
import de.itd.tracking.winslow.node.MemInfo;
import de.itd.tracking.winslow.node.Node;
import de.itd.tracking.winslow.node.NodeInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nonnull private final String name;

    @Nullable private List<UnixProcStatParser.CpuTimes> prevCpuTimes = null;

    public UnixNode(@Nonnull String name) {
        this.name = name;
    }

    private static Path resolveCpuInfo() {
        return PROC.resolve("cpuinfo");
    }

    private static Path resolveStat() {
        return PROC.resolve("stat");
    }

    private static Path resolveMemInfo() {
        return PROC.resolve("meminfo");
    }

    @Nonnull
    @Override
    public String getName() {
        return this.name;
    }

    @Nonnull
    @Override
    public NodeInfo loadInfo() throws IOException {
        var cpuInfo = loadCpuInfo();
        var memInfo = loadMemInfo();
        return new NodeInfo(name, cpuInfo, memInfo);
    }

    @Nonnull
    private CpuInfo loadCpuInfo() throws IOException {
        var model       = loadCpuModel();
        var utilization = loadCpuUtilization();
        return new CpuInfo(model, utilization);
    }

    @Nonnull
    private static String loadCpuModel() throws IOException {
        return Files
                .lines(resolveCpuInfo())
                .filter(line -> line.startsWith(CPU_INFO_MODEL_PREFIX))
                .map(line -> line.split(CPU_INFO_SEPARATOR, 2)[1].trim())
                .findFirst()
                .orElseThrow(() -> new IOException("Invalid cpu info file format"));
    }

    @Nonnull
    private List<Float> loadCpuUtilization() throws IOException {
        var current  = getCpuTimes(resolveStat());
        var previous = this.prevCpuTimes != null ? this.prevCpuTimes : current;


        var entries = Math.min(current.size(), previous.size());
        var result  = new ArrayList<Float>(entries);
        for (int i = 0; i < entries; ++i) {
            result.add(current.get(i).getChangeSince(previous.get(i)).getUtilization());
        }

        this.prevCpuTimes = current;
        return result;
    }

    private static List<UnixProcStatParser.CpuTimes> getCpuTimes(@Nonnull Path path) throws IOException {
        List<String> lines      = Files.lines(path).collect(Collectors.toUnmodifiableList());
        String[]     linesArray = new String[lines.size()];
        return new UnixProcStatParser(lines.toArray(linesArray)).getCpuTimes();
    }

    @Nonnull
    private static MemInfo loadMemInfo() throws IOException {
        return new UnixMemInfoParser(Files.lines(resolveMemInfo())).parseMemInfo();
    }
}
