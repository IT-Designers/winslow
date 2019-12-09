package de.itdesigners.winslow;

import de.itdesigners.winslow.api.project.LogEntry;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.LockedOutputStream;
import de.itdesigners.winslow.project.LogWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Executor {

    public static final String PREFIX = "[winslow] ";
    public static final Logger LOG    = Logger.getLogger(Executor.class.getSimpleName());

    @Nonnull private final String             pipeline;
    @Nonnull private final String             stage;
    @Nonnull private final Orchestrator       orchestrator;
    @Nonnull private final LockedOutputStream logOutput;
    @Nonnull private final LockHeart          lockHeart;

    @Nonnull private final List<Consumer<LogEntry>> logConsumer                = new ArrayList<>();
    @Nonnull private final List<Runnable>           shutdownListeners          = new ArrayList<>();
    @Nonnull private final List<Runnable>           shutdownCompletedListeners = new ArrayList<>();

    @Nullable private StageHandle stageHandle;

    private BlockingDeque<LogEntry> logBuffer   = new LinkedBlockingDeque<>();
    private boolean                 keepRunning = true;
    private boolean                 failed      = true;

    public Executor(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull Orchestrator orchestrator) throws LockException, FileNotFoundException {
        this.pipeline     = pipeline;
        this.stage        = stage;
        this.orchestrator = orchestrator;
        this.logOutput    = orchestrator.getLogRepository().getRawOutputStream(pipeline, stage);
        this.lockHeart    = new LockHeart(logOutput.getLock());

        var thread = new Thread(this::run);
        thread.setName(pipeline + "." + stage + ".executor");
        thread.setDaemon(false);
        thread.start();
    }

    public void setStageHandle(@Nonnull StageHandle handle) {
        this.stageHandle = handle;
    }

    public void logInf(@Nonnull String message) {
        log(false, message);
    }

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

    private Iterator<LogEntry> getIterator() {
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
                return !Executor.this.logBuffer.isEmpty() || (logs != null ? logs.hasNext() : Executor.this.keepRunning());
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
                var iter    = getIterator();
                var backoff = new Backoff(250, 950, 2f);

                LogWriter
                        .writeTo(logOutput)
                        .source(Stream.concat(
                                Stream.<LogEntry>iterate(
                                        null,
                                        p -> this.keepRunning() && iter.hasNext() && !lockHeart.hasFailed(),
                                        p -> {
                                            var next = iter.next();
                                            if (next == null) {
                                                backoff.sleep();
                                                next = iter.next();
                                            } else {
                                                backoff.reset();
                                            }
                                            return next;
                                        }
                                ).filter(Objects::nonNull),
                                Stream
                                        .of((Supplier<LogEntry>) () -> createLogEntry(false, "Done"))
                                        .map(Supplier::get)
                        ))
                        .addConsumer(this::notifyLogConsumer)
                        .runInForeground();

                logOutput.flush();
                if (!failed) {
                    orchestrator.getRunInfoRepository().setLogRedirectionCompletedSuccessfullyHint(stage);
                }
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Log writer failed", e);
            } finally {
                this.logBuffer = null;
                this.notifyShutdownListeners();
                this.logOutput.getLock().release();
            }
        } finally {
            this.notifyShutdownCompletedListeners();
        }
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

    public synchronized void stop() {
        this.keepRunning = false;
    }

    public synchronized void fail() {
        this.failed      = true;
        this.keepRunning = false;
    }

    private synchronized void notifyLogConsumer(@Nonnull LogEntry entry) {
        this.logConsumer.forEach(consumer -> consumer.accept(entry));
    }

    public synchronized void addLogEntryConsumer(@Nonnull Consumer<LogEntry> consumer) {
        this.logConsumer.add(consumer);
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
        if (this.logBuffer == null) {
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
        if (this.logBuffer == null) {
            runnable.run();
        } else {
            this.shutdownCompletedListeners.add(runnable);
        }
    }

    public boolean isRunning() {
        return this.logBuffer != null;
    }
}
