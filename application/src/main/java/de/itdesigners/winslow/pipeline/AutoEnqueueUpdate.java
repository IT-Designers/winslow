package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class AutoEnqueueUpdate implements PipelineUpdater.NoAccessUpdater {

    private static final Logger LOG = Logger.getLogger(AutoEnqueueUpdate.class.getSimpleName());

    private final @Nonnull Consumer<Pipeline> toEnqueue;

    private AutoEnqueueUpdate(@Nonnull Consumer<Pipeline> toEnqueue) {
        this.toEnqueue = toEnqueue;
    }


    @Nonnull
    public static Optional<PipelineUpdater.NoAccessUpdater> search(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        try {
            ensureAllPreconditionsAreMet(orchestrator, projectId, pipelineReadOnly);
            return Optional.of(new AutoEnqueueUpdate(toEnqueue));
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for pipeline update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static void ensureAllPreconditionsAreMet(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        ensureIsNotLocked(orchestrator, projectId);
        ensureNoElectionIsRunning(orchestrator, projectId);
        ensureHasNoRunningStages(pipelineReadOnly);
        ensureNoActiveExecutionGroupOrActiveGroupIsExhausted(pipelineReadOnly);
        ensureQueueIsEmpty(pipelineReadOnly);
    }

    @Nonnull
    @Override
    public Optional<PipelineUpdater.ExclusiveAccessUpdater> update(@Nonnull Orchestrator _orchestrator) {
        return Optional.of((orchestrator, pipeline) -> {
            if (pipeline != null) {
                try {
                    ensureAllPreconditionsAreMet(orchestrator, pipeline.getProjectId(), pipeline);
                    toEnqueue.accept(pipeline);
                } catch (PreconditionNotMetException e) {
                    LOG.log(Level.SEVERE, "At least one precondition is no longer met, cannot perform update", e);
                }
            }
            return pipeline;
        });
    }
}
