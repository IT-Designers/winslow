package de.itd.tracking.winslow.node;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.itd.tracking.winslow.node.NodeInfoUpdater.TMP_FILE_SUFFIX;

public class NodeRepository extends BaseRepository {

    private static final Logger  LOG                = Logger.getLogger(NodeRepository.class.getSimpleName());
    private static final long    ACTIVE_MAX_MS_DIFF = 5_000;
    private static final Pattern COMPILE            = Pattern.compile(".");

    public NodeRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getNodesDirectory();
    }

    private Stream<Path> listActiveNodePaths() {
        try {
            return Files
                    .list(getRepositoryDirectory())
                    .filter(p -> !p.toString().endsWith(TMP_FILE_SUFFIX))
                    .filter(p -> System.currentTimeMillis() - p.toFile().lastModified() < ACTIVE_MAX_MS_DIFF);
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
    public Optional<NodeInfo> getNodeInfo(@Nonnull String name) {
        try (var paths = listActiveNodePaths()) {
            return paths
                    .filter(p -> p.getFileName().toString().startsWith(name))
                    .findFirst()
                    .flatMap(p -> getUnsafe(p, defaultReader(NodeInfo.class)));
        }
    }
}
