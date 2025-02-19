package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class StageElectionUpdate implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private static final Logger LOG = Logger.getLogger(StageElectionUpdate.class.getSimpleName());

    private final @Nonnull String projectId;

    public StageElectionUpdate(@Nonnull String projectId) {
        this.projectId = projectId;
    }

    @Nonnull
    public static Optional<PipelineUpdater.NoAccessUpdater> check(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        try {
            // be a bit more cautious and deny any locks
            ensureIsNotLocked(orchestrator, projectId);
            ensureAllPreconditionsAreMet(orchestrator, projectId, pipelineReadOnly);
            if (pipelineReadOnly != null && orchestrator
                    .isCapableOfExecutingNextStage(pipelineReadOnly)
                    .orElse(Boolean.FALSE)) {
                if (orchestrator.hasResourcesToExecuteNextStage(pipelineReadOnly).orElse(Boolean.FALSE)) {
                    return Optional.of(new StageElectionUpdate(projectId));
                } else {
                    orchestrator.addProjectThatNeedsToBeReEvaluatedOnceMoreResourcesAreAvailable(projectId);
                }
            }
            return Optional.empty();
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for stage update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static void ensureAllPreconditionsAreMet(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        ensureIsNotLockedByAnotherInstance(orchestrator, projectId);
        ensureNoElectionIsRunning(orchestrator, projectId);
        ensureHasStageDefinitionToDeploy(pipelineReadOnly);

        ensureAnyActiveExecutionGroupHasRemainingStageExecutions(pipelineReadOnly);

        ensureNotPaused(pipelineReadOnly);
        ensureNoStageInPreparationPhase(pipelineReadOnly);
    }

    private static void ensureNoStageInPreparationPhase(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var hasPreparingStage = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .map(g -> g.getStages().anyMatch(s -> s.getState() == State.PREPARING))
                .findFirst()
                .orElse(Boolean.FALSE);
        if (hasPreparingStage) {
            throw new PreconditionNotMetException("ExecutionGroup has stage in Preparing state");
        }
    }

    private static void ensureNotPaused(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var paused = Optional
                .ofNullable(pipelineReadOnly)
                .map(Pipeline::isPauseRequested)
                .orElse(Boolean.FALSE);
        if (paused) {
            throw new PreconditionNotMetException("Pipeline is paused");
        }
    }

    @Nonnull
    @Override
    public Optional<PipelineUpdater.ExclusiveAccessUpdater> update(@Nonnull Orchestrator orchestrator) {
        return Optional.of(this);
    }

    @Nullable
    @Override
    public Pipeline update(@Nonnull Orchestrator orchestrator, @Nullable Pipeline pipeline) {
        try {
            ensureAllPreconditionsAreMet(orchestrator, projectId, pipeline);
            // TODO use the DelayedExecutor a scheduler or something more clever?
            var thread = new Thread(() -> {
                try {
                    var duration = 10_000L;
                    var puffer   = 500L;
                    if (orchestrator.getElectionManager().maybeStartElection(projectId, duration + puffer)) {
                        LockBus.ensureSleepMs(duration);
                        orchestrator.getElectionManager().closeElection(projectId);
                    }
                } catch (LockException | IOException e) {
                    LOG.log(Level.SEVERE, "Failed to have an election for project " + projectId);
                }
            });
            thread.setName(getClass().getSimpleName());
            thread.start();
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.SEVERE, "At least one precondition is no longer met, cannot start election", e);
        }
        // no changes on pipeline
        return null;
    }
}
