package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NodeParser {

    private static Logger LOG = Logger.getLogger(NodeParser.class.getSimpleName());

    @Nonnull private final Path directory;

    public NodeParser(@Nonnull Path directory) {
        this.directory = directory;
    }

    @Nonnull
    public Optional<NodeInfo> getNode(@Nonnull String name) {
        try {
            var cpuInfo = loadCpuInfo(name);
            var memInfo = loadMemInfo(name);
            return Optional.of(new NodeInfo(name, cpuInfo, memInfo));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Path resolve(@Nonnull String name, @Nonnull String component) {
        return directory.resolve(name + "." + component);
    }

    private Path resolveCpuInfo(@Nonnull String name) {
        return resolve(name, "cpuinfo");
    }

    private Path resolveCpuStat0(@Nonnull String name) {
        return resolve(name, "stat.0");
    }

    private Path resolveCpuStat1(@Nonnull String name) {
        return resolve(name, "stat.1");
    }

    private Path resolveMemInfo(@Nonnull String name) {
        return resolve(name, "meminfo");
    }

    @Nonnull
    private CpuInfo loadCpuInfo(@Nonnull String name) throws IOException {
        var model       = loadCpuModel(name);
        var utilization = loadCpuUtilization(name);
        return new CpuInfo(model, utilization);
    }

    @Nonnull
    private String loadCpuModel(@Nonnull String name) throws IOException {
        return Files
                .lines(resolveCpuInfo(name))
                .filter(line -> line.startsWith("model name"))
                .map(line -> line.split(":", 2)[1].trim())
                .findFirst()
                .orElseThrow(() -> new IOException("Invalid cpu info file format"));
    }

    private List<UnixProcStatParser.CpuTimes> getCpuTimes(@Nonnull Path path) throws IOException {
        List<String> lines      = Files.lines(path).collect(Collectors.toUnmodifiableList());
        String[]     linesArray = new String[lines.size()];
        return new UnixProcStatParser(lines.toArray(linesArray)).getCpuTimes();
    }

    @Nonnull
    private List<Float> loadCpuUtilization(@Nonnull String name) throws IOException {
        var times0 = getCpuTimes(resolveCpuStat0(name));
        var times1 = getCpuTimes(resolveCpuStat1(name));

        var result = new ArrayList<Float>(times0.size());
        for (int i = 0; i < times0.size(); ++i) {
            result.add(times1.get(i).getChangeSince(times0.get(i)).getUtilization());
        }
        return result;
    }

    @Nonnull
    private MemInfo loadMemInfo(@Nonnull String name) throws IOException {
        return new UnixMemInfoParser(Files.lines(resolveMemInfo(name))).parseMemInfo();
    }
}
