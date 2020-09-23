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
import java.nio.file.WatchKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

public class NodeRepository extends BaseRepository {

    private static final Logger LOG                = Logger.getLogger(NodeRepository.class.getSimpleName());
    private static final String TEMP_FILE_PREFIX   = ".";
    private static final long   ACTIVE_MAX_MS_DIFF = 5_000;

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
                var directory    = getRepositoryDirectory();
                var watchService = directory.getFileSystem().newWatchService();
                getRepositoryDirectory().register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                var queue = new ArrayDeque<Pair<String, Long>>();

                while (true) {
                    Optional
                            .ofNullable(watchService.poll(1, TimeUnit.SECONDS))
                            .filter(WatchKey::isValid)
                            .stream()
                            .peek(key -> notifyListeners(queue, key))
                            .forEach(WatchKey::reset);


                    while (Optional
                            .ofNullable(queue.peek())
                            .filter(e -> System.currentTimeMillis() - e.getValue1() >= ACTIVE_MAX_MS_DIFF)
                            .isPresent()) {
                        notifyListenersAboutDeletion(queue.pop().getValue0());
                    }

                }

            } catch (IOException | InterruptedException e) {
                LOG.log(Level.SEVERE, "Watcher thread failed", e);
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void notifyListenersAboutDeletion(String removed) {
        synchronized (listeners) {
            listeners.forEach(listener -> listener.accept(
                    ChangeEvent.ChangeType.DELETE,
                    removed
            ));
        }
    }

    private void notifyListeners(@Nonnull Deque<Pair<String, Long>> cacheQueue, @Nonnull WatchKey key) {
        if (!listeners.isEmpty()) {
            synchronized (listeners) {
                key
                        .pollEvents()
                        .stream()
                        .filter(e -> e.context() instanceof Path
                                && !((Path) e.context())
                                .getFileName()
                                .toString()
                                .startsWith(TEMP_FILE_PREFIX)
                        )
                        .forEach(event -> {
                            var path = (Path) event.context();
                            var name = path.getFileName().toString();
                            var type = (ChangeEvent.ChangeType) null;

                            if (cacheQueue.removeIf(p -> p.getValue0().equals(name))) {
                                type = ChangeEvent.ChangeType.UPDATE;
                            } else {
                                type = ChangeEvent.ChangeType.CREATE;
                            }

                            cacheQueue.add(new Pair<>(name, System.currentTimeMillis()));

                            final var fType = type;
                            listeners.forEach(listener -> listener.accept(fType, name));
                        });
            }
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
                    .filter(p -> !p.toString().startsWith(TEMP_FILE_PREFIX))
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
        return listActiveNodePaths().map(this::readNode);
    }

    @Nonnull
    public Optional<NodeInfo> getNodeInfo(@Nonnull String name) {
        return listActiveNodePaths()
                .filter(p -> p.getFileName().toString().equals(name))
                .findFirst()
                .map(this::readNode);
    }

    @Nonnull
    private NodeInfo readNode(@Nonnull Path p) {
        // hardcoded Toml because NodeInfoUpdate also uses hardcoded Toml...
        return new Toml().read(p.toFile()).<NodeInfo>to(NodeInfo.class);
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
