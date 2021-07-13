package de.itdesigners.winslow.node;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.api.node.NodeUtilization;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.web.websocket.ChangeEvent;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeRepository extends BaseRepository {

    private static final Logger LOG = Logger.getLogger(NodeRepository.class.getSimpleName());

    private static final String TEMP_FILE_PREFIX     = ".";
    public static final  String CSV_SUFFIX_SEPARATOR = ".";
    public static final  String CSV_SUFFIX           = ".csv";

    private static final long ACTIVE_NODE_MAX_AGE_MS = Duration.ofSeconds(15).toMillis();
    public static final  long MAX_RETENTION_TIME_MS  = Duration.ofDays(7).toMillis();

    // ext4 has 12 indirect block pointers to 4kib
    // public static final long CSV_MAGIC_FILE_SIZE_LIMIT = 12 * 4 * 1024;
    // ext4 has 12 direct and 128 indirect block pointers to 4kib
    public static final long CSV_MAGIC_FILE_SIZE_LIMIT = (12 + 128) * 4 * 1024;


    private final @Nonnull List<BiConsumer<ChangeEvent.ChangeType, String>> listeners       = new ArrayList<>();
    private final @Nonnull Map<String, Path>                                utilizationPath = new HashMap<>();


    public NodeRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        this.startWatcherDaemon();
    }

    public void addChangeListener(@Nonnull BiConsumer<ChangeEvent.ChangeType, String> listener) {
        synchronized (listeners) {
            this.listeners.add(listener);
        }
    }

    public void removeChangeListener(@Nonnull BiConsumer<ChangeEvent.ChangeType, String> listener) {
        synchronized (listeners) {
            this.listeners.remove(listener);
        }
    }

    private void startWatcherDaemon() {
        var thread = new Thread(() -> {
            try {
                // NFS does not support WatchServices... ... ... ... y tho :/
                var lastModified = new HashMap<String, Long>();
                var directory    = getRepositoryDirectory();
                var queue        = new LinkedList<String>();

                while (true) {
                    try (var stream = Files.list(directory)) {
                        stream
                                .filter(p -> !p.getFileName().toString().startsWith(TEMP_FILE_PREFIX))
                                .map(p -> new Pair<>(p.getFileName().toString(), p.toFile().lastModified()))
                                .filter(p -> !lastModified.containsKey(p.getValue0()) || p.getValue1() > lastModified.get(
                                        p.getValue0()))
                                .forEach(p -> {
                                    queue.remove(p.getValue0());
                                    queue.push(p.getValue0()); // move it to the end

                                    var type = lastModified.containsKey(p.getValue0())
                                               ? ChangeEvent.ChangeType.CREATE
                                               : ChangeEvent.ChangeType.UPDATE;
                                    notifyListeners(type, p.getValue0());
                                    lastModified.put(p.getValue0(), p.getValue1());
                                });

                    }


                    while (Optional
                            .ofNullable(queue.peek())
                            .filter(e -> System.currentTimeMillis() - lastModified.get(e) >= ACTIVE_NODE_MAX_AGE_MS)
                            .isPresent()) {
                        var node = queue.pop();
                        lastModified.remove(node);
                        notifyListeners(ChangeEvent.ChangeType.DELETE, node);
                    }
                    LockBus.ensureSleepMs(1000);
                }

            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Watcher thread failed", e);
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void notifyListeners(@Nonnull ChangeEvent.ChangeType type, @Nonnull String node) {
        synchronized (listeners) {
            listeners.forEach(listener -> listener.accept(type, node));
        }
    }


    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getNodesDirectory();
    }

    private Stream<Path> listActiveNodePaths() {
        try (var files = Files.list(getRepositoryDirectory())) {
            return files
                    .filter(p -> {
                        var fileName = p.getFileName().toString();
                        return Files.isRegularFile(p)
                                && !fileName.startsWith(TEMP_FILE_PREFIX)
                                && !fileName.endsWith(CSV_SUFFIX);
                    })
                    .filter(p -> System.currentTimeMillis() - p.toFile().lastModified() < ACTIVE_NODE_MAX_AGE_MS)
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list nodes", e);
            return Stream.empty();
        }
    }

    @Nonnull
    public Stream<String> listActiveNodes() {
        return listActiveNodePaths().map(Path::getFileName).map(Path::toString);
    }

    @Nonnull
    public Stream<NodeInfo> loadActiveNodes() {
        return listActiveNodePaths().flatMap(p -> this.readNode(p).stream());
    }

    @Nonnull
    public Optional<NodeInfo> getNodeInfo(@Nonnull String name) {
        return listActiveNodePaths()
                .filter(p -> p.getFileName().toString().equals(name))
                .findFirst()
                .flatMap(this::readNode);
    }

    @Nonnull
    private Optional<NodeInfo> readNode(@Nonnull Path p) {
        try {
            // hardcoded Toml because NodeInfoUpdate also uses hardcoded Toml...
            return Optional.of(new Toml().read(p.toFile()).<NodeInfo>to(NodeInfo.class));
        } catch (Throwable t) {
            // if the file was not found, the FileNotFoundException is thrown encapsulated in a RuntimeException... :/
            LOG.log(Level.WARNING, "Failed to read node for path=" + p, t);
            return Optional.empty();
        }
    }

    public synchronized void updateNodeInfo(@Nonnull NodeInfo node) throws IOException {
        var name = Path.of(node.getName()).getFileName().toString();
        var path = getRepositoryDirectory().resolve(name);
        var temp = getRepositoryDirectory().resolve(TEMP_FILE_PREFIX + name);
        new TomlWriter().write(node, temp.toFile());
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Path getNodeUtilizationLogDirectory(@Nonnull String nodeName) {
        return getRepositoryDirectory().resolve(nodeName + "-log");
    }

    private Path getUtilizationLogPath(@Nonnull String nodeName, long time) {
        return getNodeUtilizationLogDirectory(nodeName).resolve(nodeName + CSV_SUFFIX_SEPARATOR + time + CSV_SUFFIX);
    }

    public void updateUtilizationLog(@Nonnull NodeInfo info) throws IOException {
        updateUtilizationLog(info.getName(), NodeUtilization.from(info));
    }

    public synchronized void updateUtilizationLog(
            @Nonnull String nodeName,
            @Nonnull NodeUtilization utilization) throws IOException {
        var line = (utilization.toCsvLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        var path = Optional
                .ofNullable(this.utilizationPath.get(nodeName))
                .filter(p -> {
                    try {
                        if (Files.size(p) + line.length <= CSV_MAGIC_FILE_SIZE_LIMIT) {
                            return true;
                        }
                    } catch (Throwable t) {
                        LOG.log(Level.FINE, "Failed to retrieve file size for " + p, t);
                    }
                    return false;
                })
                .orElseGet(() -> {
                    var p = getUtilizationLogPath(nodeName, utilization.time);
                    try {
                        Files.createDirectories(p.getParent());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Failed to create directory for utilization log file: " + p, e);
                    }
                    return p;
                });


        Files.write(
                path,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.SYNC,
                StandardOpenOption.DSYNC
        );

        this.utilizationPath.put(nodeName, path);
        deleteOldUtilizationLogs(nodeName);
    }

    private void deleteOldUtilizationLogs(@Nonnull String nodeName) {
        var logs = listNodeUtilizationLogs(nodeName);
        var retainedElementCount = logs
                .stream()
                .sorted(Comparator.reverseOrder())
                .takeWhile(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis()
                                + MAX_RETENTION_TIME_MS
                                > System.currentTimeMillis();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to get last modified time for " + path, e);
                        return true;
                    }
                })
                .peek(p -> logs.remove(logs.size() - 1))
                .count();

        if (retainedElementCount > 0) {
            logs.forEach(p -> {
                try {
                    LOG.fine("Deleting utilization log " + p);
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to delete log file " + p, e);
                }
            });
        }
    }

    @Nonnull
    private List<Path> listNodeUtilizationLogs(@Nonnull String nodeName) {
        try (var files = Files.list(getNodeUtilizationLogDirectory(nodeName))) {
            return files.collect(Collectors.toList());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list nodes", e);
            return Collections.emptyList();
        }
    }

    @Nonnull
    public Stream<NodeUtilization> getNodeUtilizationBetween(
            @Nonnull String nodeName,
            long timeStart,
            long timeEnd,
            long chunkSpanMillis
    ) {
        var stream = listNodeUtilizationLogsBetween(nodeName, timeStart, timeEnd)
                .flatMap(path -> {
                    try {
                        return Files.lines(path, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to read lines of " + path, e);
                        return Stream.empty();
                    }
                })
                .map(NodeUtilization::fromCsvLineNoThrows)
                .flatMap(Optional::stream)
                .filter(u -> u.time >= timeStart && u.time <= timeEnd)
                .sorted(Comparator.comparingLong(a -> a.time));
        if (chunkSpanMillis > 1) {
            var raw     = stream.collect(Collectors.toUnmodifiableList());
            var chunked = new ArrayList<List<NodeUtilization>>();

            if (!raw.isEmpty()) {
                long chunkStart = raw.get(0).time;
                var  chunkList  = new ArrayList<NodeUtilization>();
                chunked.add(chunkList);

                for (var value : raw) {
                    if (value.time < chunkStart + chunkSpanMillis) {
                        chunkList.add(value);
                    } else {
                        chunkStart = value.time;
                        chunkList  = new ArrayList<>();
                        chunkList.add(value);
                        chunked.add(chunkList);
                    }
                }
            }

            return chunked
                    .stream()
                    .filter(list -> !list.isEmpty())
                    .map(list -> NodeUtilization.average(list.get(0).time, list.get(0).uptime, list));
        } else {
            return stream;
        }
    }

    @Nonnull
    private Stream<Path> listNodeUtilizationLogsBetween(@Nonnull String nodeName, long timeStart, long timeEnd) {
        return listNodeUtilizationLogs(nodeName)
                .stream()
                .sorted()
                .filter(p -> {
                    try {
                        var lastModify = Files.getLastModifiedTime(p).toMillis();
                        var timeCreate = extractTimestampFromUtilizationLogFileName(
                                nodeName,
                                p.getFileName().toString()
                        );
                        var startWithinRange         = timeCreate >= timeStart && timeCreate <= timeEnd;
                        var endWithinRange           = lastModify >= timeStart && lastModify <= timeEnd;
                        var rangeIsWithinStartAndEnd = timeStart >= timeCreate && timeEnd <= lastModify;
                        return startWithinRange || endWithinRange || rangeIsWithinStartAndEnd;
                    } catch (Throwable t) {
                        LOG.log(Level.WARNING, "Failed to determine modify or create time: " + p, t);
                        return false;
                    }
                });
    }


    private long extractTimestampFromUtilizationLogFileName(@Nonnull String nodeName, @Nonnull String fileName) {
        var timestamp = fileName.substring(
                nodeName.length() + CSV_SUFFIX_SEPARATOR.length(),
                fileName.length() - CSV_SUFFIX.length()
        );
        return Long.parseLong(timestamp);
    }
}
