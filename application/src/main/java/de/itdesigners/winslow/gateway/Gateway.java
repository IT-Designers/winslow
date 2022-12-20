package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public abstract class Gateway implements Runnable {

    protected @Nonnull final Queue<LogEntry> logs  = new ConcurrentLinkedQueue<>();
    protected @Nonnull       State           state = State.PREPARING;

    @Override
    public final void run() {
        this.state = State.RUNNING;
        try {
            execute();
            // TODO only for debugging
            while (!logs.isEmpty()) {
                Thread.sleep(1_000L);
            }
            this.state = State.SUCCEEDED;
        } catch (Throwable t) {
            this.state = State.FAILED;
            log(Level.SEVERE, "Gateway failed to execute", t);
        }
    }

    public abstract void execute();

    /**
     * Logs an internal message with the given level
     *
     * @param level   Categorization of the log
     * @param message Message to log
     * @param t       {@link Throwable} to append to the logs
     */
    public void log(@Nonnull Level level, @Nonnull String message, @Nonnull Throwable t) {
        var baos = new ByteArrayOutputStream();
        try (var ps = new PrintStream(baos)) {
            t.printStackTrace(ps);
        }
        log(level, message);
        baos.toString(StandardCharsets.UTF_8).lines().forEach(line -> log(level, line));
    }

    /**
     * Logs an internal message with the given level
     *
     * @param level   Categorization of the log
     * @param message Message to log
     */
    public void log(@Nonnull Level level, @Nonnull String message) {
        if (level.intValue() == Level.INFO.intValue()) {
            this.logs.add(LogEntry.stdout(message));
        } else if (level.intValue() > Level.INFO.intValue()) {
            this.logs.add(LogEntry.stderr(message));
        }
    }
}
