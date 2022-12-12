package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.ProperlyClosedDebugHelper;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Lock implements AutoCloseable, Closeable {

    public static final int    DEFAULT_LOCK_DURATION_MS = Env.lockDurationMs();
    public static final Logger LOG                      = Logger.getLogger(Lock.class.getSimpleName());

    private final @Nonnull ProperlyClosedDebugHelper helper = new ProperlyClosedDebugHelper();

    private final @Nonnull LockBus lockBus;
    private final          long    durationMs;

    private @Nonnull Token   token;
    private          boolean released = false;

    public Lock(@Nonnull LockBus lockBus, @Nonnull String subject) throws LockException {
        this(lockBus, subject, DEFAULT_LOCK_DURATION_MS);
    }

    public Lock(@Nonnull LockBus lockBus, @Nonnull String subject, long durationMs) throws LockException {
        this(lockBus, lockBus.lock(subject, durationMs), durationMs);
    }

    public Lock(@Nonnull LockBus lockBus, @Nonnull Token token, long durationMs) {
        this.lockBus    = lockBus;
        this.token      = token;
        this.durationMs = durationMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getTimeMsUntilInvalid() {
        return durationMs - (System.currentTimeMillis() - this.token.getTime());
    }

    public long getTimeUntilRenewalOnHeartbeat() {
        return durationMs / 2 - (System.currentTimeMillis() - this.token.getTime());
    }

    public synchronized void heartbeatIfNotReleased() throws LockException {
        if (this.isAlive()) {
            this.heartbeat();
        }
    }

    public synchronized void heartbeat() throws LockException {
        var time = System.currentTimeMillis();
        if (this.token.getTime() + durationMs < time) {
            throw new LockException("Lock expired");
        } else if (this.token.getTime() + (durationMs / 3) < time) {
            this.token = this.lockBus.extend(this.token, durationMs);
        }
    }

    public synchronized void waitForRelease() {
        while (!this.released) {
            try {
                this.wait(1_000);
                this.released = !this.lockBus.isLockedByThisInstance(this.token.getSubject());
                LOG.info("Lock not released yet, token=" + token);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void release() {
        if (!this.released) {
            this.released = this.lockBus.release(this.token);
            this.notifyAll();
        } else {
            LOG.log(
                    Level.WARNING,
                    "Tried to release an already released lock",
                    new RuntimeException("See the the stack trace")
            );
        }
    }

    public boolean isAlive() {
        return !this.released;
    }

    @Override
    public void close() {
        this.helper.close();
        this.release();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{token=" + this.token + "}#" + hashCode();
    }
}
