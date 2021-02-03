package de.itdesigners.winslow.node;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.web.websocket.ChangeEvent;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeRepository extends BaseRepository {

    private static final Logger LOG                = Logger.getLogger(NodeRepository.class.getSimpleName());
    private static final String TEMP_FILE_PREFIX   = ".";
    private static final long   ACTIVE_MAX_MS_DIFF = 15_000;

    private final @Nonnull List<BiConsumer<ChangeEvent.ChangeType, String>> listeners = new ArrayList<>();

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
                            .filter(e -> System.currentTimeMillis() - lastModified.get(e) >= ACTIVE_MAX_MS_DIFF)
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
                    .filter(p -> !p.getFileName().toString().startsWith(TEMP_FILE_PREFIX))
                    .filter(p -> System.currentTimeMillis() - p.toFile().lastModified() < ACTIVE_MAX_MS_DIFF)
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

    public void updateNodeInfo(@Nonnull NodeInfo node) throws IOException {
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
}
