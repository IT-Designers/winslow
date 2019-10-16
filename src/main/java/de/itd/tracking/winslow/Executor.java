package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.LockedOutputStream;
import de.itd.tracking.winslow.project.LogWriter;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Executor {

    public static final String PREFIX = "[winslow] ";
    public static final Logger LOG    = Logger.getLogger(Executor.class.getSimpleName());

    @Nonnull private final String             pipeline;
    @Nonnull private final String             stage;
    @Nonnull private final Orchestrator       orchestrator;
    @Nonnull private final Runnable           onFinished;
    @Nonnull private final LockedOutputStream logOutput;
    @Nonnull private final LockHeart          lockHeart;

    @Nonnull private final List<Consumer<LogEntry>> logConsumer = new ArrayList<>();

    private BlockingDeque<LogEntry> logBuffer   = new LinkedBlockingDeque<>();
    private boolean                 keepRunning = true;

    public Executor(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull Orchestrator orchestrator,
            @Nonnull Runnable onFinished) throws LockException, FileNotFoundException {
        this.pipeline     = pipeline;
        this.stage        = stage;
        this.orchestrator = orchestrator;
        this.onFinished   = onFinished;
        this.logOutput    = orchestrator.getLogRepository().getRawOutputStream(pipeline, stage);
        this.lockHeart    = new LockHeart(logOutput.getLock());

        var thread = new Thread(this::run);
        thread.setName(pipeline + "." + stage + ".executor");
        thread.setDaemon(false);
        thread.start();
    }

    public void logInf(@Nonnull String message) {
        log(false, message);
    }

    public void logErr(@Nonnull String message) {
        log(true, message);
    }

    private synchronized void log(boolean error, @Nonnull String message) {
        if (logBuffer != null) {
            logBuffer.add(new LogEntry(
                    System.currentTimeMillis(),
                    LogEntry.Source.MANAGEMENT_EVENT,
                    error,
                    PREFIX + message
            ));
        } else {
            LOG.warning("LogBuffer gone, not able to log message (error=" + error + "): " + message);
        }
    }

    private Iterator<LogEntry> getIterator() throws IOException {
        return new CombinedIterator<>(
                this.orchestrator.getBackend().getLogs(pipeline, stage),
                new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return Executor.this.logBuffer != null && !Executor.this.logBuffer.isEmpty();
                    }

                    @Override
                    public LogEntry next() {
                        return Executor.this.logBuffer.poll();
                    }
                }
        );
    }

    private void run() {
        try (lockHeart; logOutput) {
            var iter    = getIterator();
            var backoff = new Backoff(250, 950, 2f);

            LogWriter
                    .writeTo(logOutput)
                    .source(Stream.<LogEntry>iterate(
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
                    ).filter(Objects::nonNull))
                    .addConsumer(this::notifyLogConsumer)
                    .runInForeground();

            logOutput.flush();
            orchestrator.getRunInfoRepository().setLogRedirectionCompletedSuccessfullyHint(pipeline, stage);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Log writer failed", e);
        } finally {
            this.logBuffer = null;
            this.onFinished.run();
            this.logOutput.getLock().release();
        }
    }

    private synchronized boolean keepRunning() {
        return this.keepRunning;
    }

    public synchronized void flush() {
        while (!this.logBuffer.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void stop() {
        this.keepRunning = false;
        this.logOutput.getLock().release();
    }


    private synchronized void notifyLogConsumer(@Nonnull LogEntry entry) {
        this.logConsumer.forEach(consumer -> consumer.accept(entry));
    }

    public synchronized void addLogEntryConsumer(@Nonnull Consumer<LogEntry> consumer) {
        this.logConsumer.add(consumer);
    }
}
