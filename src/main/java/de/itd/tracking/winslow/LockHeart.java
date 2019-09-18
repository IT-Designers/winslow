package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.Lock;
import de.itd.tracking.winslow.fs.LockException;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockHeart implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(LockHeart.class.getSimpleName());

    @Nonnull private final Lock    lock;
    @Nonnull private final Thread  thread;
    private                boolean failed      = false;
    private                boolean keepBeating = true;
    private                boolean isBeating   = true;

    public LockHeart(@Nonnull Lock lock) {
        this.lock = lock;
        this.thread = new Thread(this::beatIt);
        this.thread.setDaemon(true);
        this.thread.setName(this.toString());
        this.thread.start();
    }

    private void beatIt() {
        try {
            while (this.keepBeating && !lock.isReleased()) {
                var sleepTimeMs = lock.getTimeUntilRenewalOnHeartbeat();
                if (sleepTimeMs > 0) {
                    try {
                        Thread.sleep(sleepTimeMs);
                    } catch (InterruptedException e) {
                        LOG.log(Level.WARNING, "Sleep got interrupted, might cause performance issues", e);
                    }
                }
                lock.heartbeatIfNotReleased();
            }
        } catch (LockException e) {
            LOG.log(Level.SEVERE, "Heart stopped beating unexpectedly", e);
            this.failed = true;
        } finally {
            this.isBeating = false;
        }
    }

    public void stop() {
        this.keepBeating = false;
    }

    public boolean stopAndJoin() {
        while (isBeating) {
            try {
                this.stop();
                this.thread.join();
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, "Got interrupted on joining, might cause performance issues", e);
            }
        }
        return !hasFailed();
    }

    public boolean hasFailed() {
        return failed;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{lock=" + this.lock + "}#" + hashCode();
    }

    @Override
    public void close() {
        this.stop();
    }
}
