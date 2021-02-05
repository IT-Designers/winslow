package de.itdesigners.winslow.node;

import de.itdesigners.winslow.fs.LockBus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeInfoUpdater implements Runnable {

    private static final long   SLEEP_TIME_MS = 1_000;
    private static final Logger LOG           = Logger.getLogger(NodeInfoUpdater.class.getSimpleName());

    @Nonnull private final NodeRepository repository;
    @Nonnull private final Node           node;

    private long waitUntil = 0L;

    private NodeInfoUpdater(@Nonnull NodeRepository repository, @Nonnull Node node) {
        this.repository = repository;
        this.node       = node;
    }

    public static NodeInfoUpdater spawn(@Nonnull NodeRepository repository, @Nonnull Node node) {
        var updater = new NodeInfoUpdater(repository, node);
        var thread  = new Thread(updater);
        thread.setName(NodeInfoUpdater.class.getSimpleName() + "." + node.getName());
        thread.setDaemon(true);
        thread.start();
        return updater;
    }

    public void update() throws IOException {
        repository.updateNodeInfo(node.loadInfo());
        waitUntil = System.currentTimeMillis() + SLEEP_TIME_MS;
    }

    public void updateNoThrows() {
        try {
            update();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to update NodeInfo", e);
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            var currentTimeMillis = System.currentTimeMillis();

            if (waitUntil < currentTimeMillis) {
                updateNoThrows();
            }
            if (waitUntil >= currentTimeMillis) {
                var toSleep = waitUntil - currentTimeMillis;
                LockBus.ensureSleepMs(Math.max(1, toSleep));
            }
        }
    }
}
