package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class CommonUpdateConstraints {


    public static void ensureIsNotLocked(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId) throws PreconditionNotMetException {
        PreconditionNotMetException.requireFalse(
                "Pipeline is currently locked",
                orchestrator
                        .getPipelines()
                        .getPipeline(projectId)
                        .isLocked()
        );
    }


    public static void ensureIsNotLockedByAnotherInstance(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId) throws PreconditionNotMetException {
        PreconditionNotMetException.requireFalse(
                "Pipeline is currently locked (by another instance)",
                orchestrator
                        .getPipelines()
                        .getPipeline(projectId)
                        .isLockedByAnotherInstance()
        );
    }

    public static void ensureNoElectionIsRunning(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId) throws PreconditionNotMetException {
        PreconditionNotMetException.requireFalse(
                "There is currently an election for this pipeline",
                orchestrator
                        .getElectionManager()
                        .getElection(projectId)
                        .isPresent()
        );
    }

    public static void ensureHasNoRunningStages(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var empty = Optional
                .ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .stream()
                .flatMap(ExecutionGroup::getRunningStages)
                .findAny()
                .isEmpty();
        if (!empty) {
            throw new PreconditionNotMetException("The pipeline is currently executing at least one stage");
        }
    }

    public static void ensureNoActiveExecutionGroupOrActiveGroupIsExhaustedOrHasFailed(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var depleted = Optional
                .ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .map(g -> !hasRemainingOrRunningStageExecutions(g))
                .orElse(Boolean.TRUE);
        if (!depleted) {
            throw new PreconditionNotMetException("ExecutionGroup is not exhausted");
        }
    }

    public static void ensureArchivableOrRetrievableExecutionGroup(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        if (pipelineReadOnly != null) {
            var stillRelevant = pipelineReadOnly
                    .getActiveExecutionGroup()
                    .map(CommonUpdateConstraints::hasRemainingOrRunningStageExecutions);

            if (stillRelevant.orElse(Boolean.FALSE)) {
                throw new PreconditionNotMetException("Pipeline still has an relevant active ExecutionGroup");
            }

            if (stillRelevant.isEmpty() && !pipelineReadOnly.hasEnqueuedStages()) {
                throw new PreconditionNotMetException(
                        "Pipeline neither can archive active nor retrieve next ExecutionGroup");
            }
        } else {
            throw new PreconditionNotMetException("No Pipeline, no update");
        }
    }

    public static Boolean hasEnqueuedStages(@Nullable Pipeline pipelineReadOnly) {
        return Optional
                .ofNullable(pipelineReadOnly)
                .map(Pipeline::hasEnqueuedStages)
                .orElse(Boolean.FALSE);
    }

    public static boolean hasRemainingOrRunningStageExecutions(@Nonnull ExecutionGroup group) {
        return (group.hasRemainingExecutions() || group.getRunningStages().count() > 0)
                && group.getStages().noneMatch(s -> s.getState() == State.Failed);
    }
    public static void ensureActiveExecutionGroupHasRemainingStageExecutions(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        if (!hasActiveExecutionGroupRemainingExecutions(pipelineReadOnly)) {
            throw new PreconditionNotMetException("Active ExecutionGroup has no remaining stage executions");
        }
    }

    public static Boolean hasActiveExecutionGroupRemainingExecutions(@Nullable Pipeline pipelineReadOnly) {
        return Optional
                .ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .map(g -> g.hasRemainingExecutions() && g.getStages().noneMatch(s -> s.getState() == State.Failed))
                .orElse(Boolean.FALSE);
    }

    public static Boolean isActiveExecutionGroupStillRelevant(@Nullable Pipeline pipelineReadOnly) {
        return Optional
                .ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .map(CommonUpdateConstraints::hasRemainingOrRunningStageExecutions)
                .orElse(Boolean.FALSE);
    }

    public static void ensureHasStageDefinitionToDeploy(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        Optional.ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .flatMap(ExecutionGroup::getNextStageDefinition)
                .orElseThrow(() -> new PreconditionNotMetException("No stage definition to deploy a stage from"));
    }

    public static void ensureQueueIsEmpty(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var empty = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getEnqueuedExecutions)
                .findAny()
                .isEmpty();
        if (!empty) {
            throw new PreconditionNotMetException("There is at least one entry in the queue for this pipeline");
        }
    }

    public static void ensureIsNotPaused(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var paused = Optional
                .ofNullable(pipelineReadOnly)
                .map(Pipeline::isPauseRequested)
                .orElse(Boolean.FALSE);
        if (paused) {
            throw new PreconditionNotMetException("The pipeline is paused");
        }
    }
}
