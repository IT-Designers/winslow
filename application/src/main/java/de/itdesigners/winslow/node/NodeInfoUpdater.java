package de.itdesigners.winslow.node;

import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.api.node.NodeUtilization;
import de.itdesigners.winslow.fs.LockBus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NodeInfoUpdater implements Runnable {

    private static final long     SLEEP_TIME_MS = 1_000;
    private static final Logger   LOG           = Logger.getLogger(NodeInfoUpdater.class.getSimpleName());
    private static final Duration SUM_DURATION  = Duration.ofMinutes(1);

    @Nonnull private final NodeRepository repository;
    @Nonnull private final Node           node;

    private @Nonnull List<NodeInfo> summedInfo = new ArrayList<>((int) SUM_DURATION.toSeconds() + 10);
    private          long           waitUntil  = 0L;

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
        var info = node.loadInfo();
        waitUntil = info.time() + SLEEP_TIME_MS;
        repository.updateNodeInfo(info);

        summedInfo.add(info);

        var firstEntry = summedInfo.get(0);
        var recentEntry= summedInfo.get(summedInfo.size() -1);

        if (firstEntry.time() < (System.currentTimeMillis() - SUM_DURATION.toMillis())) {
            var util = NodeUtilization.average(
                    firstEntry.time(),
                    recentEntry.uptime(),
                    summedInfo.stream().map(NodeUtilization::from).collect(Collectors.toList())
            );
            repository.updateUtilizationLog(info.name(), util);
            summedInfo.clear();
        }

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
