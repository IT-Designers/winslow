package de.itdesigners.winslow.node;

import de.itdesigners.winslow.fs.LockBus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeInfoUpdater implements Runnable {

    private static final Logger LOG = Logger.getLogger(NodeInfoUpdater.class.getSimpleName());

    @Nonnull private final NodeRepository repository;
    @Nonnull private final Node           node;

    private NodeInfoUpdater(@Nonnull NodeRepository repository, @Nonnull Node node) {
        this.repository = repository;
        this.node      = node;
    }

    public static void spawn(@Nonnull NodeRepository repository, @Nonnull Node node) {
        var updater = new NodeInfoUpdater(repository, node);
        var thread  = new Thread(updater);
        thread.setName(NodeInfoUpdater.class.getSimpleName() + "." + node.getName());
        thread.setDaemon(true);
        thread.start();
    }



    @Override
    public void run() {
        while (true) {
            try {
                repository.updateNodeInfo(node.loadInfo());
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to update NodeInfo", e);
                e.printStackTrace();
            } finally {
                LockBus.ensureSleepMs(1_000);
            }
        }
    }
}
