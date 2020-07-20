package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class CommonUpdateConstraints {


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

    public static void ensureNoActiveExecutionGroupOrActiveGroupIsExhausted(@Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        var depleted = Optional
                .ofNullable(pipelineReadOnly)
                .flatMap(Pipeline::getActiveExecutionGroup)
                .map(g -> !g.hasRemainingExecutions())
                .orElse(Boolean.TRUE);
        if (!depleted) {
            throw new PreconditionNotMetException("ExecutionGroup is not exhausted");
        }
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
