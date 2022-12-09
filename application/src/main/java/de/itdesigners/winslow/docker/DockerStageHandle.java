package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerStageHandle implements StageHandle {

    private static final Logger LOG = Logger.getLogger(DockerStageHandle.class.getSimpleName());

    private static final @Nonnull String  DOCKER_DEFAULT_TAG           = ":latest";
    private static final @Nonnull String  DOCKER_TAG_SEPARATOR         = ":";
    private static final          int     DOCKER_LOGS_TIMESTAMP_LENGTH = 30;
    private static final          boolean DOCKER_LOGS_WITH_TIMESTAMP   = true;

    private final @Nonnull DockerBackend backend;
    private final @Nonnull String        stageId;

    private final @Nonnull Deque<LogEntry> logs      = new ConcurrentLinkedDeque<>();
    private final @Nonnull Set<Closeable>  closeable = new HashSet<>();

    private           boolean started     = false;
    private           boolean gone        = false;
    private @Nullable State   state       = State.Preparing;
    private @Nullable Stats   stats       = null;
    private @Nullable String  containerId = null;

    public DockerStageHandle(
            @Nonnull DockerBackend backend,
            @Nonnull String stageId,
            @Nonnull CreateContainerCmd createContainerCmd) {
        this.backend = backend;
        this.stageId = stageId;

        runAndCatchRuntimeExceptionsInNewThread(() -> {
            pullImageAndThenStartContainer(createContainerCmd.getImage());
            containerId = createContainer(createContainerCmd);
            setupStatsListener(containerId);
            startContainer(containerId);
            setupLogListener(containerId);
        });
    }

    private void onListenerFailed() {
        this.gone  = true;
        this.state = State.Failed;
    }

    private void runAndCatchRuntimeExceptionsInNewThread(@Nonnull Runnable fn) {
        var thread = new Thread(() -> runAndCatchRuntimeExceptions(fn));
        thread.setName(getClass().getSimpleName() + "-" + this.stageId);
        thread.start();
    }

    private void runAndCatchRuntimeExceptions(@Nonnull Runnable fn) {
        try {
            fn.run();
        } catch (RuntimeException e) {
            logErr((e.getCause() != null ? e.getCause() : e).getClass().getSimpleName() + ": " + e.getMessage());
            this.state = State.Failed;
            LOG.log(Level.WARNING, "Execution encountered unexpected error", e);
        }
    }

    private void pullImageAndThenStartContainer(@Nullable String containerImage) throws DockerException {
        var image = Optional
                .ofNullable(containerImage)
                .map(i -> {
                    if (!i.contains(DOCKER_TAG_SEPARATOR)) {
                        return i + DOCKER_DEFAULT_TAG;
                    } else {
                        return i;
                    }
                })
                .orElse("");

        try (var cmd = backend.getDockerClient().pullImageCmd(image)) {
            cmd.exec(new ResultCallback.Adapter<>() {
                @Override
                public void onStart(Closeable stream) {
                    super.onStart(stream);
                    DockerStageHandle.this.closeable.add(this);
                }

                @Override
                public void onNext(PullResponseItem object) {
                    if (object.getProgressDetail() != null) {
                        logOut(
                                object.getStatus() + (
                                        object.getProgressDetail().getCurrent() != null && object
                                                .getProgressDetail()
                                                .getTotal() != null
                                        ? (String.format(
                                                "%6.1f %%, ",
                                                (object
                                                        .getProgressDetail()
                                                        .getCurrent()
                                                        .doubleValue() / object
                                                        .getProgressDetail()
                                                        .getTotal()
                                                        .doubleValue()) * 100.0
                                        )
                                                + object.getProgressDetail().getCurrent()
                                                + " of "
                                                + object.getProgressDetail().getTotal()

                                        )
                                        : ""
                                )
                        );
                    } else if (object.getStatus() != null) {
                        logOut(object.getStatus());
                    }
                }

                @Override
                public void onComplete() {
                    super.onComplete();
                    DockerStageHandle.this.closeable.remove(this);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private String createContainer(@Nonnull CreateContainerCmd createContainerCmd) {
        try (createContainerCmd) {
            var createResult = createContainerCmd.exec();
            var containerId  = createResult.getId();

            if (createResult.getWarnings() != null) {
                Arrays.asList(createResult.getWarnings()).forEach(this::logErr);
            }

            return containerId;
        }
    }

    private void setupLogListener(@Nonnull String containerId) {
        try (var cmd = this.backend.getDockerClient().logContainerCmd(containerId)) {
            cmd
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .withTimestamps(DOCKER_LOGS_WITH_TIMESTAMP)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onStart(Closeable stream) {
                            super.onStart(stream);
                            DockerStageHandle.this.closeable.add(this);
                            DockerStageHandle.this.started = true;
                            DockerStageHandle.this.state   = State.Running;
                        }

                        @Override
                        public void onNext(Frame frame) {
                            var message = new String(frame.getPayload(), StandardCharsets.UTF_8).trim();
                            var time = DOCKER_LOGS_WITH_TIMESTAMP
                                       ? Instant
                                               .parse(message.substring(0, DOCKER_LOGS_TIMESTAMP_LENGTH))
                                               .toEpochMilli()
                                       : System.currentTimeMillis();
                            message = DOCKER_LOGS_WITH_TIMESTAMP
                                      ? message.substring(DOCKER_LOGS_TIMESTAMP_LENGTH).trim()
                                      : message;

                            if (frame.getStreamType() == StreamType.STDERR || frame.getStreamType() == StreamType.STDOUT) {
                                log(
                                        time,
                                        LogEntry.Source.STANDARD_IO,
                                        frame.getStreamType() == StreamType.STDERR,
                                        message
                                );
                            } else {
                                log(message, frame.getStreamType() == StreamType.STDERR);
                            }
                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                            DockerStageHandle.this.closeable.remove(this);
                            runAndCatchRuntimeExceptionsInNewThread(() -> {
                                try (var cmd = DockerStageHandle.this.backend
                                        .getDockerClient()
                                        .waitContainerCmd(containerId)) {
                                    cmd
                                            .exec(new Adapter<>() {
                                                @Override
                                                public void onStart(Closeable stream) {
                                                    super.onStart(stream);
                                                    DockerStageHandle.this.closeable.add(this);
                                                }

                                                @Override
                                                public void onNext(WaitResponse object) {
                                                    if (object.getStatusCode() == 0) {
                                                        DockerStageHandle.this.state = State.Succeeded;
                                                    } else {
                                                        DockerStageHandle.this.state = State.Failed;
                                                        DockerStageHandle.this.gone  = object.getStatusCode() == null;
                                                        logErr("Non successful exit code: " + object.getStatusCode());
                                                    }
                                                }

                                                @Override
                                                public void onComplete() {
                                                    super.onComplete();
                                                    DockerStageHandle.this.closeable.remove(this);
                                                }
                                            })
                                            .awaitCompletion();
                                } catch (InterruptedException | RuntimeException e) {
                                    logErr("Exit code could not be retrieved, container cleaned up too fast");
                                    LOG.log(Level.WARNING, "Failed to wait for container result", e);
                                    DockerStageHandle.this.state = State.Failed;
                                    DockerStageHandle.this.gone  = true;
                                    if (e instanceof RuntimeException re) {
                                        throw re;
                                    } else {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            super.onError(throwable);
                            LOG.log(Level.WARNING, "Log listener failed", throwable);
                            DockerStageHandle.this.onListenerFailed();
                        }
                    })
                    .awaitStarted();
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Failed to start stats listener", e);
            throw new RuntimeException(e);
        } catch (DockerException e) {
            LOG.log(Level.WARNING, "Failed to start log listener", e);
            throw e;
        }
    }

    private void startContainer(@Nonnull String containerId) {
        try (var cmd = this.backend.getDockerClient().startContainerCmd(containerId)) {
            cmd.exec();
        } catch (DockerException e) {
            LOG.log(Level.WARNING, "Failed to start container", e);
            throw e;
        }
    }

    private void setupStatsListener(@Nonnull String containerId) {
        try (var cmd = this.backend.getDockerClient().statsCmd(containerId)) {
            cmd
                    .withNoStream(false)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onStart(Closeable stream) {
                            super.onStart(stream);
                            DockerStageHandle.this.closeable.add(this);
                        }

                        @Override
                        public void onNext(Statistics stats) {
                            // https://github.com/moby/moby/blob/801230ce315ef51425da53cc5712eb6063deee95/api/types/stats.go#L23
                            var totalCpuUsage = Optional
                                    .ofNullable(stats.getCpuStats())
                                    .map(CpuStatsConfig::getCpuUsage)
                                    .map(CpuUsageConfig::getTotalUsage)
                                    .orElse(0L);

                            var preCpuUsage = Optional
                                    .ofNullable(stats.getPreCpuStats())
                                    .map(CpuStatsConfig::getCpuUsage)
                                    .map(CpuUsageConfig::getTotalUsage)
                                    .orElse(0L);

                            var cpuUsageDelta            = totalCpuUsage - preCpuUsage;
                            var cpuUsagePercentPerSecond = (double) cpuUsageDelta / 1_000_000_000.0;

                            var cpuMaxFreq = (double) DockerStageHandle.this.backend
                                    .getPlatformInfo()
                                    .getCpuSingleCoreMaxFrequencyMhz()
                                    .map(Integer::doubleValue)
                                    .orElse(1_000.0); // TODO assume something ...

                            DockerStageHandle.this.stats = new Stats(
                                    DockerStageHandle.this.stageId,
                                    DockerStageHandle.this.backend.getNodeName(),
                                    (float) (cpuMaxFreq * cpuUsagePercentPerSecond),
                                    (float) cpuMaxFreq,
                                    Optional
                                            .ofNullable(stats.getMemoryStats())
                                            .map(c -> Optional
                                                    .ofNullable(c.getUsage())
                                                    .orElse(0L)
                                                    - Optional
                                                    .ofNullable(c.getStats())
                                                    .map(StatsConfig::getCache)
                                                    .orElse(0L)
                                            )
                                            .orElse(0L),
                                    Optional
                                            .ofNullable(stats.getMemoryStats())
                                            .map(MemoryStatsConfig::getLimit)
                                            .orElse(0L)
                            );

                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                            DockerStageHandle.this.closeable.remove(this);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            super.onError(throwable);
                            LOG.log(Level.WARNING, "Stats listener failed", throwable);
                            DockerStageHandle.this.onListenerFailed();
                        }
                    })
                    .awaitStarted();
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Failed to start stats listener", e);
            throw new RuntimeException(e);
        } catch (DockerException e) {
            LOG.log(Level.WARNING, "Failed to start stats listener", e);
            throw e;
        }
    }

    private void logOut(@Nonnull String message) {
        this.log(message, false);
    }

    private void logErr(@Nonnull String message) {
        this.log(message, true);
    }

    private void log(@Nonnull String message, boolean error) {
        log(
                System.currentTimeMillis(),
                LogEntry.Source.MANAGEMENT_EVENT,
                error,
                "[docker] " + message
        );
    }

    private void log(long time, @Nonnull LogEntry.Source source, boolean error, @Nonnull String message) {
        this.logs.add(new LogEntry(
                time,
                source,
                error,
                message
        ));
    }

    @Override
    public void pollNoThrows() {
        // NOP
    }

    @Override
    public void poll() {
        // NOP
    }

    @Override
    public boolean isRunning() {
        return !isGone() && hasStarted() && !hasFinished();
    }

    @Override
    public boolean hasStarted() {
        return this.started;
    }

    @Override
    public boolean hasFinished() {
        return this.logs.isEmpty() && (hasFailed() || hasSucceeded());
    }

    @Override
    public boolean hasFailed() {
        return this.logs.isEmpty() && getState().stream().anyMatch(s -> State.Failed == s);
    }

    @Override
    public boolean hasSucceeded() {
        return this.logs.isEmpty() && getState().stream().anyMatch(s -> State.Succeeded == s);
    }

    @Override
    public boolean isGone() {
        return this.gone;
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        return Optional.ofNullable(this.state);
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !hasFinished();
            }

            @Override
            public LogEntry next() {
                if (DockerStageHandle.this.logs.isEmpty()) {
                    return null;
                } else {
                    return DockerStageHandle.this.logs.pop();
                }
            }
        };
    }

    @Nonnull
    @Override
    public Optional<Stats> getStats() {
        return Optional.ofNullable(this.stats);
    }

    @Override
    public void stop() throws IOException {
        if (!isGone() && !hasFinished()) {
            if (containerId == null) {
                throw new IOException("Container cannot be stopped, not started yet");
            } else {
                runAndCatchRuntimeExceptions(() -> {
                    try (var cmd = this.backend.getDockerClient().stopContainerCmd(this.containerId)) {
                        cmd.exec();
                    }
                });
            }
        }
    }

    @Override
    public void kill() throws IOException {
        if (!isGone() && !hasFinished()) {
            if (containerId == null) {
                throw new IOException("Container cannot be stopped, not started yet");
            } else {
                runAndCatchRuntimeExceptions(() -> {
                    try (var cmd = this.backend.getDockerClient().killContainerCmd(this.containerId)) {
                        cmd.exec();
                    }
                });
            }
        }
    }


    @Override
    public void close() throws IOException {
        for (var closeable : this.closeable) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                log(e.getMessage(), true);
                LOG.log(Level.WARNING, "Failed to close " + closeable, e);
            }
        }
        this.gone |= !this.hasFinished();
        this.closeable.clear();
    }
}
