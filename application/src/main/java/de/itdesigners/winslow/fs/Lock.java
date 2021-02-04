package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.Env;

import java.io.Closeable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Lock implements Closeable {

    public static final int    DEFAULT_LOCK_DURATION_MS = Env.lockDurationMs();
    public static final Logger LOG                      = Logger.getLogger(Lock.class.getSimpleName());

    private final LockBus lockBus;
    private final long    durationMs;

    private Token   token;
    private boolean released = false;

    public Lock(LockBus lockBus, String subject) throws LockException {
        this(lockBus, subject, DEFAULT_LOCK_DURATION_MS);
    }

    public Lock(LockBus lockBus, String subject, long durationMs) throws LockException {
        this(lockBus, lockBus.lock(subject, durationMs), durationMs);
    }

    public Lock(LockBus lockBus, Token token, long durationMs) {
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

    public synchronized boolean heartbeatIfNotReleased() throws LockException {
        if (!this.isReleased()) {
            this.heartbeat();
        }
        return this.isReleased();
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
                LOG.info("Lock not release yet, token=" + token);
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

    public boolean isReleased() {
        return this.released;
    }

    @Override
    public void close() {
        this.release();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{token=" + this.token + "}#" + hashCode();
    }
}
