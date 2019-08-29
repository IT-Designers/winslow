package de.itd.tracking.winslow.fs;

import java.io.Closeable;

public class Lock implements Closeable {

    private final LockBus lockBus;
    private final long    durationMs;

    private Token token;

    public Lock(LockBus lockBus, String subject, long durationMs) throws LockException {
        this(lockBus, lockBus.lock(subject, durationMs), durationMs);
    }

    public Lock(LockBus lockBus, Token token, long durationMs) {
        this.lockBus = lockBus;
        this.token = token;
        this.durationMs = durationMs;
    }

    public synchronized void heartbeat() throws LockException {
        var time = System.currentTimeMillis();
        if (this.token.getTime() + durationMs < time) {
            throw new LockException("Lock expired");
        } else if (this.token.getTime() + (durationMs / 2) < time) {
            this.token = this.lockBus.extend(this.token, durationMs);
        }
    }

    public synchronized void release() {
        this.lockBus.release(this.token);
    }

    @Override
    public void close() {
        this.release();
    }
}
