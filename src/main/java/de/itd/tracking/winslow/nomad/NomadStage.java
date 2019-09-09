package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.Stage;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

public class NomadStage implements Stage {

    @Nonnull private final NomadOrchestrator orchestrator;
    @Nonnull private final String            jobId;
    @Nonnull private final String            taskName;
    @Nonnull private final StageDefinition   definition;
    @Nonnull private final Date              startTime;

    @Nullable private Date finishTime;

    public NomadStage(@Nonnull NomadOrchestrator orchestrator, @Nonnull String jobId, @Nonnull String taskName, @Nonnull StageDefinition definition) {
        this.orchestrator = orchestrator;
        this.jobId        = jobId;
        this.taskName     = taskName;
        this.definition   = definition;

        this.startTime = new Date();
    }

    @Nonnull
    @Override
    public StageDefinition getDefinition() {
        return this.definition;
    }

    @Nonnull
    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Nullable
    @Override
    public Date getFinishTime() {
        return this.finishTime;
    }

    public void finishNow() {
        this.finishTime = new Date();
    }

    @Nonnull
    @Override
    public State getState() throws OrchestratorConnectionException {
        try {
            return orchestrator
                    .getJobAllocationContainingTaskState(jobId, taskName)
                    .flatMap(alloc -> NomadOrchestrator.toRunningStageState(alloc, taskName))
                    .orElse(State.Running);
        } catch (IOException | NomadException e) {
            throw new OrchestratorConnectionException("Failed to retrieve state information", e);
        }
    }

    @Override
    public Iterable<String> getStdOut(int lastNLines) {
        return () -> new LogIterator(jobId, taskName, "stdout", orchestrator.getClientApi(), () -> {
            try {
                return orchestrator.getJobAllocationContainingTaskState(jobId, taskName);
            } catch (IOException | NomadException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Iterable<String> getStdErr(int lastNLines) {
        return () -> new LogIterator(jobId, taskName, "stderr", orchestrator.getClientApi(), () -> {
            try {
                return orchestrator.getJobAllocationContainingTaskState(jobId, taskName);
            } catch (IOException | NomadException e) {
                return Optional.empty();
            }
        });
    }
}
