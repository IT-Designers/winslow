package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageDefinitionBuilder;
import de.itdesigners.winslow.config.UserInput;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class AutoEnqueueUpdate implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private static final Logger LOG = Logger.getLogger(AutoEnqueueUpdate.class.getSimpleName());

    private final @Nonnull WorkspaceConfiguration workspaceConfiguration;
    private final @Nonnull StageDefinition        stageDefinition;

    private AutoEnqueueUpdate(
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull StageDefinition stageDefinition) {
        this.workspaceConfiguration = workspaceConfiguration;
        this.stageDefinition        = stageDefinition;
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
                    .map(def -> new AutoEnqueueUpdate(def.getValue0(), def.getValue1()));
        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for pipeline update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Nonnull
    private static Optional<Pair<WorkspaceConfiguration, StageDefinition>> generateNextStageDefinition(
            @Nonnull Orchestrator orchestrator,
            @Nonnull Pipeline pipeline) {
        return orchestrator
                .getProjects()
                .getProject(pipeline.getProjectId())
                .unsafe()
                .flatMap(project -> pipeline
                        .getActiveOrPreviousExecutionGroup()
                        .filter(g -> g
                                .getStages()
                                .allMatch(s -> s.getFinishState().equals(Optional.of(State.Succeeded))))
                        .flatMap(mostRecent -> {
                            var pipelineDefinition = project.getPipelineDefinition();
                            var mostRecentStageDefIndex = guessStageIndex(
                                    pipelineDefinition,
                                    mostRecent.getStageDefinition().getName()
                            );
                            var nextStageDefinitionIndex = mostRecentStageDefIndex
                                    .map(index -> index + (mostRecent.isConfigureOnly() ? 0 : 1));

                            return nextStageDefinitionIndex
                                    .filter(index -> index < pipelineDefinition.getStages().size())
                                    .map(index -> new Pair<>(mostRecent, pipelineDefinition.getStages().get(index)));
                        })
                        .map(pair -> {
                            var prevGroup               = pair.getValue0();
                            var nextStageDefinitionBase = pair.getValue1();
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


                            return new Pair<>(
                                    prevGroup.getWorkspaceConfiguration(),
                                    builder.build()
                            );
                        }));
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
        ensureHasNoRunningStages(pipelineReadOnly);
        ensureNoActiveExecutionGroupOrActiveGroupIsExhaustedOrHasFailed(pipelineReadOnly);
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
                pipeline.enqueueSingleExecution(stageDefinition, workspaceConfiguration);

                stageDefinition.getRequires().map(UserInput::getConfirmation).ifPresent(confirmation -> {
                    var inThisCase = UserInput.Confirmation.Once == confirmation
                        && pipeline
                            .getExecutionHistory()
                            .noneMatch(g -> g.getStageDefinition().getName().equals(stageDefinition.getName()));

                    if (UserInput.Confirmation.Always == confirmation || inThisCase) {
                        pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                    }
                });

                return pipeline;
            } catch (PreconditionNotMetException e) {
                LOG.log(Level.SEVERE, "At least one precondition is no longer met, cannot perform update", e);
            }
        }
        return pipeline;
    }
}
