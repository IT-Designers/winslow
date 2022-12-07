package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.LockedOutputStream;
import de.itdesigners.winslow.project.LogWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Executor implements Closeable, AutoCloseable {

    public static final String PREFIX      = "[winslow] ";
    public static final Logger LOG         = Logger.getLogger(Executor.class.getSimpleName());
    public static final int    INTERVAL_MS = 1000;

    @Nonnull private final String             pipeline;
    @Nonnull private final String             stage;
    @Nonnull private final Orchestrator       orchestrator;
    @Nonnull private final LockedOutputStream logOutput;
    @Nonnull private final LockHeart          lockHeart;

    @Nonnull private final List<Consumer<LogEntry>> logConsumer                = new ArrayList<>();
    @Nonnull private final List<Runnable>           shutdownListeners          = new ArrayList<>();
    @Nonnull private final List<Runnable>           shutdownCompletedListeners = new ArrayList<>();
    @Nonnull private final IntervalInvoker          intervalInvoker            = new IntervalInvoker(INTERVAL_MS);

    @Nullable private StageHandle stageHandle;

    private BlockingDeque<LogEntry> logBuffer   = new LinkedBlockingDeque<>();
    private boolean                 keepRunning = true;
    private boolean                 failed      = false;
    private boolean                 killed      = false;

    public Executor(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull Orchestrator orchestrator) throws LockException, FileNotFoundException {
        this.pipeline     = pipeline;
        this.stage        = stage;
        this.orchestrator = orchestrator;
        this.logOutput    = orchestrator.getLogRepository().getRawOutputStream(pipeline, stage);
        this.lockHeart    = new LockHeart(logOutput.getLock(), () -> {
            try {
                logErr("LockHeart stopped unexpectedly");
                this.kill();
            } catch (IOException e) {
                logErr("Failed to kill stage execution: " + e);
            } finally {
                this.fail();
            }
        });

        this.intervalInvoker.addListener(this::statsUpdater);

        var thread = new Thread(this::run);
        thread.setName(pipeline + "." + stage + ".exctr");
        thread.setDaemon(false);
        thread.start();
    }

    private void killNoThrows() {
        try {
            this.kill();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to kill stage execution", e);
        }
    }

    public void kill() throws IOException {
        try {
            logErr("Received KILL signal");
            if (this.stageHandle != null) {
                this.stageHandle.kill();
            }
        } finally {
            this.killed = true;
        }
    }

    public boolean hasBeenKilled() {
        return this.killed;
    }

    private void statsUpdater() {
        var handle = this.stageHandle;
        if (handle != null) {
            try {
                handle.getStats().ifPresent(value -> orchestrator.getRunInfoRepository().setStats(stage, value));
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to update stats for " + stage, e);
            }
        }
    }

    public void setStageHandle(@Nonnull StageHandle handle) {
        this.stageHandle = handle;
    }

    /**
     * Logs an internal info message
     *
     * @param message Message to log
     */
    public void logInf(@Nonnull String message) {
        log(false, message);
    }

    /**
     * Logs an internal error message
     *
     * @param message Message to log
     */
    public void logErr(@Nonnull String message) {
        log(true, message);
    }

    private synchronized void log(boolean error, @Nonnull String message) {
        if (logBuffer != null) {
            logBuffer.add(createLogEntry(error, message));
        } else {
            LOG.warning("LogBuffer gone, not able to log message (error=" + error + "): " + message);
        }
    }

    private LogEntry createLogEntry(boolean error, @Nonnull String message) {
        return new LogEntry(
                System.currentTimeMillis(),
                LogEntry.Source.MANAGEMENT_EVENT,
                error,
                PREFIX + message
        );
    }

    private Iterator<LogEntry> getLogIterator() {
        return new Iterator<>() {
            Iterator<LogEntry> logs = null;

            private void retrieveLogs() {
                if (logs == null && Executor.this.stageHandle != null) {
                    try {
                        logs = Executor.this.stageHandle.getLogs();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public boolean hasNext() {
                retrieveLogs();
                return !Executor.this.logBuffer.isEmpty()
                        || (logs != null ? logs.hasNext() : Executor.this.keepRunning());
            }

            @Override
            public LogEntry next() {
                retrieveLogs();
                var next = logBuffer.poll();
                if (next == null && logs != null && logs.hasNext()) {
                    next = logs.next();
                }
                return next;
            }
        };
    }

    private void run() {
        try (lockHeart) {
            try (logOutput) {
                var iter    = getLogIterator();
                var backoff = new Backoff(250, 950, 2f);

                LogWriter
                        .writeTo(logOutput)
                        .source(Stream.concat(
                                Stream.<LogEntry>iterate(
                                        null,
                                        p -> p != null || (this.keepRunning() && iter.hasNext() && !lockHeart.hasFailed()),
                                        p -> {
                                            intervalInvoker.maybeInvokeAll();
                                            var next = iter.next();
                                            if (next == null) {
                                                backoff.sleep(intervalInvoker.timeMillisUntilNextInvocation());
                                                next = iter.next();
                                            } else {
                                                backoff.reset();
                                            }
                                            return next;
                                        }
                                ).filter(Objects::nonNull),
                                Stream
                                        .of((Supplier<LogEntry>) () -> {
                                            var stageHandle = this.stageHandle;
                                            var failed  = stageHandle != null && stageHandle.hasFailed();
                                            var gone    = stageHandle != null && stageHandle.isGone();
                                            var message = failed ? "Failed" : (gone ? "Gone" : "Done");
                                            return createLogEntry(failed || gone, message);
                                        })
                                        .map(Supplier::get)
                        ))
                        .addConsumer(this::notifyLogConsumerIfStdout)
                        .runInForeground();

                logOutput.flush();
                if (executionFinishedSuccessfully()) {
                    orchestrator.getRunInfoRepository().setLogRedirectionCompletedSuccessfullyHint(stage);
                }
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Log writer failed", e);
                this.killNoThrows();
            } finally {
                this.logBuffer = null;
                this.notifyShutdownListeners();
                this.logOutput.getLock().release();
            }
        } finally {
            this.notifyShutdownCompletedListeners();
            try {
                if (this.stageHandle != null) {
                    this.stageHandle.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean executionFinishedSuccessfully() {
        return !failed && Optional
                .ofNullable(stageHandle)
                .flatMap(StageHandle::getState)
                // try a second time
                .or(() -> Optional.ofNullable(stageHandle).flatMap(StageHandle::getState))
                .orElse(State.Failed) == State.Succeeded;
    }

    @Nonnull
    public String getPipeline() {
        return pipeline;
    }

    @Nonnull
    public String getStage() {
        return stage;
    }

    private synchronized boolean keepRunning() {
        return this.keepRunning || !this.logBuffer.isEmpty();
    }

    public synchronized void stop() throws IOException {
        this.keepRunning = false;
        if (this.stageHandle != null) {
            this.stageHandle.stop();
        }
    }

    public synchronized void fail() {
        this.failed      = true;
        this.keepRunning = false;
    }

    private synchronized void notifyLogConsumerIfStdout(@Nonnull LogEntry entry) {
        if (LogEntry.Source.STANDARD_IO == entry.getSource()) {
            this.logConsumer.forEach(consumer -> consumer.accept(entry));
        }
    }

    /**
     * Adds the given consumer to the list of consumers to be notified on <b>STDIO</b> logs.
     * Internal logs (like {@link de.itdesigners.winslow.api.pipeline.LogEntry.Source#MANAGEMENT_EVENT})
     * are not passed to the given consumer.
     *
     * @param consumer {@link Consumer} to add
     */
    public synchronized void addLogEntryConsumer(@Nonnull Consumer<LogEntry> consumer) {
        this.logConsumer.add(consumer);
    }

    public synchronized void removeLogEntryConsumer(@Nonnull Consumer<LogEntry> consumer) {
        this.logConsumer.remove(consumer);
    }

    private synchronized void notifyShutdownListeners() {
        for (var runnable : this.shutdownListeners) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Shutdown listener failed", t);
            }
        }
        this.shutdownListeners.clear();
    }

    private synchronized void notifyShutdownCompletedListeners() {
        for (var runnable : this.shutdownCompletedListeners) {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Shutdown listener failed", t);
            }
        }
        this.shutdownCompletedListeners.clear();
    }

    /**
     * @param runnable Listener being called immediately if already shutdown or when the executor is stopping
     *                 but while the stage is still locked
     */
    public synchronized void addShutdownListener(@Nonnull Runnable runnable) {
        if (!isRunning()) {
            runnable.run();
        } else {
            this.shutdownListeners.add(runnable);
        }
    }

    /**
     * @param runnable Listener being called immediately if already shutdown or when the executor is stopping
     *                 and after the lock of the stage has been released
     */
    public synchronized void addShutdownCompletedListener(@Nonnull Runnable runnable) {
        if (!isRunning()) {
            runnable.run();
        } else {
            this.shutdownCompletedListeners.add(runnable);
        }
    }

    public boolean isRunning() {
        return this.logBuffer != null;
    }

    /**
     * Kills the execution of this {@link Executor} and closes it ({@link #kill()} followed by {@link #close()}).
     * Logs {@link IOException}s thrown by {@link #abort()}.
     */
    public void abortNoThrows() {
        try {
            this.abort();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to abort execution of " + stage, e);
        }
    }

    /**
     * Kills the execution of this {@link Executor} and closes it ({@link #kill()} followed by {@link #close()}).
     *
     * @throws IOException If killing or closing failed
     */
    public void abort() throws IOException {
        try (this) {
            this.kill();
        } finally {
            this.fail();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.stageHandle != null) {
            if (this.stageHandle.isRunning()) {
                LOG.warning("Closing Executor on running StageHandle " + this.stage);
            }
            this.stageHandle.close();
        }
    }
}
