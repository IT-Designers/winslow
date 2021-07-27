package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.Evaluation;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.CombinedIterator;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;
import de.itdesigners.winslow.pipeline.StageId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class NomadStageHandle implements StageHandle {

    private static final Logger LOG                              = Logger.getLogger(NomadStageHandle.class.getSimpleName());
    private static final int    PARTIAL_FAILURE_IS_STAGE_FAILURE = 2;
    private static final long   KILL_TIMEOUT_MS                  = 30_000;
    private static final long   SHUTDOWN_DURATION_MS             = 15_000;
    private static final long   GONE_TIMEOUT                     = 10_000;

    private final @Nonnull NomadBackend backend;
    private final @Nonnull StageId      stageId;

    private @Nullable String    allocationId;
    private @Nullable TaskState taskState;
    private @Nullable State     state = null;

    private boolean gone                = false;
    private Long    goneTime            = null;
    private Long    killTime            = null;
    private Long    shutdownDelayedTime = null;

    private int partialErrorCounter = 0;

    public NomadStageHandle(@Nonnull NomadBackend backend, @Nonnull StageId stageId) {
        this.backend = backend;
        this.stageId = stageId;
    }

    @Nonnull
    public StageId getStageId() {
        return stageId;
    }

    @Nonnull
    public String getFullyQualifiedStageId() {
        return stageId.getFullyQualified();
    }

    @Nonnull
    public Optional<String> getAllocationId() {
        return Optional.ofNullable(allocationId);
    }

    @Nonnull
    public Optional<TaskState> getTaskState() {
        return Optional.ofNullable(taskState);
    }

    @Override
    public void pollNoThrows() {
        try {
            this.poll();
        } catch (Throwable t) {
            this.notifyAboutPartialFailure(t);
        }
    }

    @Override
    public void poll() throws IOException {
        if (!gone) {
            this.backend
                    .getAllocation(getFullyQualifiedStageId())
                    .ifPresentOrElse(alloc -> {
                        this.allocationId = alloc.getId();

                        var taskStates = alloc.getTaskStates();
                        var taskState  = taskStates != null ? taskStates.get(getFullyQualifiedStageId()) : null;

                        if (taskState != null) {
                            this.taskState = taskState;
                            this.state     = NomadBackend.toRunningStageState(taskState);

                            if (NomadBackend.hasTaskFinished(taskState) && this.shutdownDelayedTime == null) {
                                this.shutdownDelayedTime = System.currentTimeMillis() + SHUTDOWN_DURATION_MS;
                            }

                            resetGoneIndicators();
                        } else {
                            this.detectedIndicatorForGone();
                        }
                    }, this::detectedIndicatorForGone);
        }
    }

    private void resetGoneIndicators() {
        this.goneTime = null;
    }

    private void detectedIndicatorForGone() {
        if (hasStarted() && goneTime == null) {
            this.goneTime = System.currentTimeMillis();
        } else if (goneTime != null && goneTime + GONE_TIMEOUT < System.currentTimeMillis()) {
            this.gone = true;
        }
    }

    @Override
    public boolean isRunning() {
        return !isGone() && State.Running == this.state;
    }

    @Override
    public boolean hasStarted() {
        return this.allocationId != null;
    }

    @Override
    public boolean hasFinished() {
        if (shutdownDelayedTime != null && shutdownDelayedTime > System.currentTimeMillis() && !isGone()) {
            return false;
        } else {
            return isGone() || hasFailed() || hasSucceeded();
        }
    }

    @Override
    public boolean hasFailed() {
        var gone    = isGone();
        var failed  = State.Failed == this.state;
        var counter = partialErrorCounter >= PARTIAL_FAILURE_IS_STAGE_FAILURE;
        return gone || failed || counter;
    }

    @Override
    public boolean hasSucceeded() {
        return State.Succeeded == this.state;
    }

    @Override
    public boolean isGone() {
        return gone || killTimeoutReached() || (this.state == null && this.killTime != null);
    }

    private boolean killTimeoutReached() {
        return killTime != null && killTime + KILL_TIMEOUT_MS < System.currentTimeMillis();
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        try {
            return this.backend.getState(this.stageId);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to retrieve stage state", e);
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return new CombinedIterator<>(
                LogStream.stdOutIter(backend.getNewClient(), this),
                LogStream.stdErrIter(backend.getNewClient(), this),
                new EventStream(this),
                new EvaluationLogger(this)
        );
    }


    @Nonnull
    @Override
    public Optional<Stats> getStats() throws IOException {
        var stageAllocation = this.backend.getAllocation(getFullyQualifiedStageId());
        if (stageAllocation.isPresent()) {
            var alloc   = stageAllocation.get();
            var allocId = alloc.getId();

            try (var client = this.backend.getNewClient()) {
                var allocation = client.getAllocationsApi().info(allocId).getValue();
                var stats      = client.getClientApi(client.getConfig().getAddress()).stats(allocId).getValue();
                // stats["DeviceStats"][0]
                //                        .InstanceStats[<some-name>]
                //                                      .Name: Quadro M2200
                //                                      .Type: gpu
                //                                      .Vendor: nvidia

                var cpu    = stats.getResourceUsage().getCpuStats();
                var memory = stats.getResourceUsage().getMemoryStats();

                return Optional.of(new Stats(
                        (float) cpu.getTotalTicks(),
                        cpu.getPercent() > 0
                        ? (float) ((cpu.getTotalTicks() / cpu.getPercent()) * 100.0)
                        : (float) allocation.getResources().getCpu(),
                        // RSS is the Resident Set Size and is used to show how much memory is allocated to that
                        // process and is in RAM. It does not include memory that is swapped out. It does include
                        // memory from shared libraries as long as the pages from those libraries are actually in
                        // memory. It does include all stack and heap memory.
                        // https://stackoverflow.com/questions/7880784/what-is-rss-and-vsz-in-linux-memory-management
                        memory.getRss().longValue(),
                        allocation.getResources().getMemoryMb() * 1024 * 1024L
                ));
            } catch (NomadException e) {
                throw new IOException("Internal nomad exception: " + e.getMessage(), e);
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing stage[" + this.stageId + "]");
        this.killSilently();
    }

    public void kill() throws IOException {
        LOG.info("Killing myself[" + stageId + "]");
        this.killSilently();
    }

    private void killSilently() throws IOException {
        if (this.killTime == null) {
            this.killTime = System.currentTimeMillis();
        }
        this.backend.kill(getFullyQualifiedStageId());
    }

    @Nonnull
    public Stream<Evaluation> getEvaluations() throws IOException {
        return this.backend.getEvaluations(getFullyQualifiedStageId());
    }

    public void notifyAboutPartialFailure(@Nonnull Throwable t) {
        try {
            this.partialErrorCounter += 1;
            LOG.log(Level.WARNING, "Got notified about partial failure, counter=" + partialErrorCounter, t);
            if (this.partialErrorCounter >= PARTIAL_FAILURE_IS_STAGE_FAILURE) {
                LOG.warning("Counter above allowed value, will escalate and kill stage");
                this.kill();
            }
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, "Failed to kill stage[" + stageId + "] caused by escalated partial failure", ioe);
        }
    }
}
