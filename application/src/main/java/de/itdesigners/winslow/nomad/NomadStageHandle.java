package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.Evaluation;
import com.hashicorp.nomad.apimodel.TaskState;
import de.itdesigners.winslow.CombinedIterator;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.project.LogEntry;
import de.itdesigners.winslow.api.project.State;

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
    private final @Nonnull String       stageId;

    private @Nullable String    allocationId;
    private @Nullable TaskState taskState;
    private @Nullable State     state = null;

    private boolean gone                = false;
    private Long    goneTime            = null;
    private Long    killTime            = null;
    private Long    shutdownDelayedTime = null;

    private int partialErrorCounter = 0;

    public NomadStageHandle(@Nonnull NomadBackend backend, @Nonnull String stageId) {
        this.backend = backend;
        this.stageId = stageId;
    }

    @Nonnull
    public String getStageId() {
        return stageId;
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
                    .getAllocation(stageId)
                    .ifPresentOrElse(alloc -> {
                        this.allocationId = alloc.getId();

                        var taskStates = alloc.getTaskStates();
                        var taskState  = taskStates != null ? taskStates.get(stageId) : null;

                        if (taskState != null) {
                            this.taskState = taskState;
                            this.state     = NomadBackend.toRunningStageState(taskState);

                            if (NomadBackend.hasTaskFinished(taskState) && this.shutdownDelayedTime != null) {
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
        if (shutdownDelayedTime != null && shutdownDelayedTime > System.currentTimeMillis()) {
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
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return new CombinedIterator<>(
                LogStream.stdOutIter(backend.getNewClientApi(), this),
                LogStream.stdErrIter(backend.getNewClientApi(), this),
                new EventStream(this),
                new EvaluationLogger(this)
        );
    }

    public void kill() throws IOException {
        if (this.killTime == null) {
            this.killTime = System.currentTimeMillis();
        }
        LOG.info("Killing myself[" + stageId + "]");
        this.backend.kill(this.stageId);
    }

    @Nonnull
    public Stream<Evaluation> getEvaluations() throws IOException {
        return this.backend.getEvaluations(this.stageId);
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
