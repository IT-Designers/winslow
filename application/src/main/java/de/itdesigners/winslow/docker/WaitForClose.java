package de.itdesigners.winslow.docker;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaitForClose implements Closeable, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(WaitForClose.class.getSimpleName());

    private final @Nonnull AtomicBoolean closed = new AtomicBoolean(false);

    public void awaitClose() {
        synchronized (closed) {
            while (!closed.get()) {
                try {
                    closed.wait(10);
                } catch (InterruptedException e) {
                    LOG.log(Level.FINE, "Got interrupted, will try again", e);
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (closed) {
            closed.set(true);
            closed.notifyAll();
        }
    }
}
