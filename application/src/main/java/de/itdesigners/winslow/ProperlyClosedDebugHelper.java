package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This helper will print a stack trace for  the location it was created at, if it was not closed properly
 * ({@link #close()}) before cleaned up by the garbage collector.
 */
public class ProperlyClosedDebugHelper implements AutoCloseable, Closeable {

    private static final Cleaner CLEANER = Cleaner.create();

    private final @Nonnull AtomicBoolean closed;

    public ProperlyClosedDebugHelper() {
        var stackTraceOrigin = new Exception(getClass().getSimpleName() + " was created here but was not closed properly!");
        var closed           = new AtomicBoolean(false);

        CLEANER.register(this, () -> {
            if (!closed.get()) {
                stackTraceOrigin.printStackTrace();
            }
        });

        this.closed = closed;
    }

    @Override
    public void close() {
        this.closed.set(true);
    }
}
