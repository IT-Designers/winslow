package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.project.Project;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class AutoEnqueueUpdate implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private static final Logger LOG = Logger.getLogger(AutoEnqueueUpdate.class.getSimpleName());

    private final @Nonnull WorkspaceConfiguration workspaceConfiguration;
    private final @Nonnull StageDefinition        stageDefinition;
    private final @Nonnull ExecutionGroup         parent;

    private AutoEnqueueUpdate(
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull ExecutionGroup parent) {
        this.workspaceConfiguration = workspaceConfiguration;
        this.stageDefinition        = stageDefinition;
        this.parent                 = parent;
    }


    @Nonnull
    public static Optional<PipelineUpdater.NoAccessUpdater> check(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        try {
            ensureAllPreconditionsAreMet(orchestrator, projectId, pipelineReadOnly);
            return Optional
                    .ofNullable(pipelineReadOnly)
                    .flatMap(p -> generateNextStageDefinition(orchestrator, p))
                    .map(def -> new AutoEnqueueUpdate(def.getValue0(), def.getValue1(), def.getValue2()));
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for pipeline update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Nonnull
    private static Optional<Triplet<WorkspaceConfiguration, StageDefinition, ExecutionGroup>> generateNextStageDefinition(
            @Nonnull Orchestrator orchestrator,
            @Nonnull Pipeline pipeline) {
        return orchestrator
                .getProjects()
                .getProject(pipeline.getProjectId())
                .unsafe()
                .flatMap(project -> pipeline
                        .getPreviousExecutionGroup()
                        .filter(g -> g
                                .getStages()
                                .allMatch(s -> s.getFinishState().equals(Optional.of(State.Succeeded))))
                        .flatMap(mostRecent -> getNextStageDefinition(project, mostRecent).map(p -> p.addAt2(mostRecent)))
                        .map(triplet -> {
                            var prevGroup               = triplet.getValue0();
                            var nextStageDefinitionBase = triplet.getValue1();
                            var env                     = new TreeMap<>(nextStageDefinitionBase.getEnvironment());
                            var builder = new StageDefinitionBuilder()
                                    .withTemplateBase(nextStageDefinitionBase)
                                    .withEnvironment(env);


                            // augment StageDefinition if there is already an
                            // execution instance of it
                            pipeline
                                    .getActiveAndPastExecutionGroups()
                                    .filter(g -> g
                                            .getStageDefinition()
                                            .getName()
                                            .equals(nextStageDefinitionBase.getName()))
                                    .filter(g -> g
                                            .getStages()
                                            .anyMatch(s -> s.getState() == State.Succeeded))
                                    .reduce((first, second) -> second) // take the most recent
                                    .ifPresent(recent -> {
                                        env.clear();
                                        env.putAll(recent
                                                           .getStages()
                                                           .filter(s -> s.getState() == State.Succeeded)
                                                           .reduce((first, second) -> second)
                                                           .orElseThrow() // would not have passed through any match
                                                           .getEnv());
                                        recent
                                                .getStageDefinition()
                                                .getImage()
                                                .ifPresent(builder::withImage);
                                    });


                            return new Triplet<>(
                                    prevGroup.getWorkspaceConfiguration(),
                                    builder.build(),
                                    triplet.getValue2()
                            );
                        }));
    }

    @Nonnull
    private static Optional<Pair<ExecutionGroup, StageDefinition>> getNextStageDefinition(
            @Nonnull Project project,
            @Nonnull ExecutionGroup mostRecent) {
        var pipelineDefinition = project.getPipelineDefinition();
        var mostRecentStageDefIndex = guessStageIndex(
                pipelineDefinition,
                mostRecent.getStageDefinition().getName()
        );
        var nextStageDefinitionIndex = mostRecentStageDefIndex
                .map(index -> index + (mostRecent.isConfigureOnly() ? 0 : 1));

        return nextStageDefinitionIndex
                .filter(index -> index < pipelineDefinition.getStages().size())
                .flatMap(index -> {
                    /*while (!pipelineDefinition.getStages().get(index).getDecision()) {
                        if (index + 1 < pipelineDefinition.getStages().size()) {
                            index++;
                        } else {
                            return Optional.empty();
                        }
                    }*/
                    return Optional.of(new Pair<>(mostRecent, pipelineDefinition.getStages().get(index)));
                });
    }

    @Nonnull
    private static ArrayList<Object> getResultsOfStages(
            @Nonnull Pipeline pipeline,
            @Nonnull List<String> stageNames) {
        var relevantStages = pipeline.getExecutionHistory()
                                     .filter(g -> stageNames.contains(g.getStageDefinition().getName()));
        var results = new ArrayList<>();
        relevantStages.forEach(g -> g.getStages().forEach(s -> results.add(s.getResult())));
        return results;
    }

    @Nonnull
    private static Optional<Integer> guessStageIndex(
            @Nonnull PipelineDefinition definition,
            @Nonnull String stageName) {
        for (int i = 0; i < definition.getStages().size(); ++i) {
            if (definition.getStages().get(i).getName().equals(stageName)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static void ensureAllPreconditionsAreMet(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) throws PreconditionNotMetException {
        ensureIsNotLockedByAnotherInstance(orchestrator, projectId);
        ensureNoElectionIsRunning(orchestrator, projectId);
        ensureActiveGroupIsEmpty(pipelineReadOnly);
        //ensureHasGroupWithNoRunningStages(pipelineReadOnly);
        //ensureNoActiveExecutionGroupOrAnyActiveGroupIsExhaustedOrHasFailed(pipelineReadOnly);
        ensureQueueIsEmpty(pipelineReadOnly);
        ensureIsNotPaused(pipelineReadOnly);
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

                var requiredConfirmation = stageDefinition
                        .getRequires()
                        .map(UserInput::getConfirmation)
                        .orElse(UserInput.Confirmation.Never);

                var inThisCase = UserInput.Confirmation.Once == requiredConfirmation
                        && pipeline
                        .getExecutionHistory()
                        .noneMatch(g -> g.getStageDefinition().getName().equals(stageDefinition.getName()));

                var requiresConfirmation = UserInput.Confirmation.Always == requiredConfirmation || inThisCase;
                var hasConfirmation = pipeline
                        .getResumeNotification()
                        .orElse(null) == Pipeline.ResumeNotification.Confirmation;

                if (requiresConfirmation && !hasConfirmation) {
                    pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                } else {
                    pipeline.enqueueSingleExecution(stageDefinition, workspaceConfiguration, "automatic", parent.getId());
                }

                return pipeline;
            } catch (PreconditionNotMetException e) {
                LOG.log(Level.SEVERE, "At least one precondition is no longer met, cannot perform update", e);
            }
        }
        return pipeline;
    }
}
