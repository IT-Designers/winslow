package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class ActiveExecutionGroupUpdate implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private static final Logger LOG = Logger.getLogger(ActiveExecutionGroupUpdate.class.getSimpleName());

    private ActiveExecutionGroupUpdate() {

    }


    @Nonnull
    public static Optional<PipelineUpdater.NoAccessUpdater> check(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        try {
            ensureAllPreconditionsAreMet(orchestrator, projectId, pipelineReadOnly);
            return Optional.of(new ActiveExecutionGroupUpdate());
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for pipeline update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static void ensureAllPreconditionsAreMet(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        ensureIsNotLockedByAnotherInstance(orchestrator, projectId);
        ensureNoElectionIsRunning(orchestrator, projectId);
        ensureHasNoRunningStages(pipelineReadOnly);
        ensureNoActiveExecutionGroupOrActiveGroupIsExhaustedOrHasFailed(pipelineReadOnly);
        ensureArchivableOrRetrievableExecutionGroup(pipelineReadOnly);
    }


    @Nonnull
    @Override
    public Optional<PipelineUpdater.ExclusiveAccessUpdater> update(@Nonnull Orchestrator _orchestrator) {
        return Optional.of(this);
    }

    @Nullable
    @Override
    public Pipeline update(@Nonnull Orchestrator orchestrator, @Nullable Pipeline pipeline) {
        if (pipeline != null) {
            try {
                ensureAllPreconditionsAreMet(orchestrator, pipeline.getProjectId(), pipeline);

                for (ExecutionGroup group : pipeline
                        .getActiveExecutionGroups()
                        .filter(executionGroup -> !isActiveExecutionGroupStillRelevant(executionGroup))
                        .collect(Collectors.toList())) {
                    pipeline.archiveActiveExecution(group);
                }

                if (pipeline.canRetrieveNextActiveExecution()) {
                    pipeline.retrieveNextActiveExecution();
                }
                return pipeline;
            } catch (PreconditionNotMetException | ExecutionGroupStillHasRunningStagesException e) {
                LOG.log(Level.SEVERE, "At least one precondition is no longer met, cannot perform update", e);
            }
        }
        return null;
    }
}
