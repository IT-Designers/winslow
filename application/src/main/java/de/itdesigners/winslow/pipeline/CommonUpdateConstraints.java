package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;

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

    public static void ensureHasGroupWithNoRunningStages(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var empty = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .filter(g -> g.getRunningStages().findAny().isPresent())
                .findAny()
                .isEmpty();
        if (!empty) {
            throw new PreconditionNotMetException(
                    "The pipeline is currently not executing a group with no active stages");
        }
    }

    public static void ensureNoActiveExecutionGroupOrAnyActiveGroupIsExhaustedOrHasFailed(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var noActive = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .findFirst().isEmpty();
        var anyExhaustedOrFailed = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .anyMatch(g -> !hasRemainingOrRunningStageExecutions(g));
        if (!noActive && !anyExhaustedOrFailed) {
            throw new PreconditionNotMetException("No ExecutionGroup or none is exhausted");
        }
    }

    public static void ensureHasArchivableOrRetrievableExecutionGroup(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        if (pipelineReadOnly != null) {
            var hasArchivable = pipelineReadOnly
                    .getActiveExecutionGroups()
                    .anyMatch(g -> !hasRemainingOrRunningStageExecutions(g));

            if (!hasArchivable && !pipelineReadOnly.hasEnqueuedStages()) {
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
        return group.getRunningStages().count() > 0 || (group.hasRemainingExecutions()
                && group.getStages().noneMatch(s -> s.getState() == State.Failed));
    }

    public static void ensureAnyActiveExecutionGroupHasRemainingStageExecutions(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        if (!hasAnyActiveExecutionGroupRemainingExecutions(pipelineReadOnly)) {
            throw new PreconditionNotMetException("Active ExecutionGroup has no remaining stage executions");
        }
    }

    public static Boolean hasAnyActiveExecutionGroupRemainingExecutions(@Nullable Pipeline pipelineReadOnly) {
        return Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .map(g -> g.hasRemainingExecutions()
                        && (doesIgnoreFailuresWithinExecutionGroup(g.getStageDefinition())
                        || g.getStages().noneMatch(s -> s.getState() == State.Failed)))
                .findFirst()
                .orElse(Boolean.FALSE);
    }

    public static boolean doesIgnoreFailuresWithinExecutionGroup(@Nonnull StageDefinition def) {
        return def instanceof StageWorkerDefinition w ? w.ignoreFailuresWithinExecutionGroup() : false;
    }

    public static Boolean isActiveExecutionGroupStillRelevant(@Nonnull ExecutionGroup executionGroup) {
        return CommonUpdateConstraints.hasRemainingOrRunningStageExecutions(executionGroup);
    }

    public static void ensureHasStageDefinitionToDeploy(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        Optional.ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .filter(g -> g.getNextStageDefinition().isPresent())
                .findFirst()
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

    public static void ensureActiveGroupIsEmpty(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var empty = Optional
                .ofNullable(pipelineReadOnly)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .findAny()
                .isEmpty();
        if (!empty) {
            throw new PreconditionNotMetException("There is at least one entry in the active group for this pipeline");
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
