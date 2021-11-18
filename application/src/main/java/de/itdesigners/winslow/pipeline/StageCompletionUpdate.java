package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.fs.ObsoleteWorkspaceFinder;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StageCompletionUpdate implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private static final @Nonnull Logger LOG = Logger.getLogger(StageCompletionUpdate.class.getSimpleName());

    private final @Nonnull List<String> stagesToCheck;

    public StageCompletionUpdate(@Nonnull List<String> stagesToCheck) {
        this.stagesToCheck = stagesToCheck;
    }

    public static Optional<StageCompletionUpdate> check(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        var stages = Stream
                .ofNullable(pipelineReadOnly)
                .flatMap(pipeline -> pipeline
                        .getActiveExecutionGroups()
                        .flatMap(ExecutionGroup::getRunningStages)
                        .map(Stage::getFullyQualifiedId))
                .filter(stageId -> !orchestrator.getLogRepository().isLocked(projectId, stageId))
                .collect(Collectors.toList());
        if (stages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new StageCompletionUpdate(stages));
        }
    }

    @Nonnull
    @Override
    public Optional<PipelineUpdater.ExclusiveAccessUpdater> update(@Nonnull Orchestrator orchestrator) {
        return Optional.of(this);
    }

    @Nullable
    @Override
    public Pipeline update(
            @Nonnull Orchestrator orchestrator,
            @Nullable Pipeline pipeline) {
        if (pipeline != null) {
            var projectId = pipeline.getProjectId();
            var changes = pipeline
                    .getActiveExecutionGroups()
                    .flatMap(ExecutionGroup::getRunningStages)
                    .filter(stage -> stagesToCheck.contains(stage.getFullyQualifiedId()))
                    .filter(stage -> !orchestrator.getLogRepository().isLocked(projectId, stage.getFullyQualifiedId()))
                    .peek(stage -> {
                        if (orchestrator
                                .getRunInfoRepository()
                                .hasLogRedirectionCompletedSuccessfullyHint(stage.getFullyQualifiedId())) {
                            Optional<Map<String, String>> result = orchestrator.getRunInfoRepository().getResult(stage.getFullyQualifiedId());
                            result.ifPresent(stage::setResult);

                            stage.finishNow(State.Succeeded);

                            var remaining = pipeline.getActiveExecutionGroups().anyMatch(ExecutionGroup::hasRemainingExecutions);
                            var singleExec = pipeline.getResumeNotification().map(n -> Pipeline.ResumeNotification.RunSingleThenPause == n).orElse(Boolean.FALSE);

                            if (!remaining && singleExec) {
                                pipeline.resetResumeNotification();
                                pipeline.requestPause();
                            }
                        } else {
                            stage.finishNow(State.Failed);
                        }
                        cleanupAfterStageExecution(orchestrator, stage.getFullyQualifiedId());
                        discardObsoleteWorkspaces(orchestrator, projectId, pipeline);
                    })
                    .count();

            if (changes > 0) {
                pipeline.getActiveExecutionGroups().forEach(active -> {
                    var hasFailed = active.getStages().anyMatch(s -> s.getState() == State.Failed);
                    var hasRemaining = active.hasRemainingExecutions();
                    var ignoreFailures = active.getStageDefinition().getIgnoreFailuresWithinExecutionGroup();

                    if ((hasFailed && !hasRemaining) || (hasFailed && !ignoreFailures)) {
                        pipeline.requestPause(Pipeline.PauseReason.StageFailure);
                    }
                });
                return pipeline;
            }
        }
        return null;
    }

    public static void cleanupAfterStageExecution(@Nonnull Orchestrator orchestrator, @Nonnull String stageId) {
        try {
            orchestrator.getRunInfoRepository().removeAllProperties(stageId);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to cleanup stage " + stageId, e);
        }
    }

    public static void discardObsoleteWorkspaces(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nonnull Pipeline pipeline) {
        var policy = pipeline
                .getDeletionPolicy()
                .or(() -> orchestrator
                        .getProjects()
                        .getProject(projectId)
                        .unsafe()
                        .map(Project::getPipelineDefinition)
                        .flatMap(PipelineDefinition::getDeletionPolicy)
                )
                .orElseGet(Orchestrator::defaultDeletionPolicy);
        var history    = pipeline.getActiveAndPastExecutionGroups().collect(Collectors.toList());
        var finder     = new ObsoleteWorkspaceFinder(policy).withExecutionHistory(history);
        var obsolete   = finder.collectObsoleteWorkspaces();
        var workspaces = orchestrator.getResourceManager();
        var purgeScope = orchestrator.getResourceManager()
                                     .getWorkspace(Orchestrator.getWorkspacePathForPipeline(pipeline));

        if (purgeScope.isEmpty()) {
            LOG.warning("Cannot determine purge scope for Pipeline with ProjectId " + pipeline.getProjectId());
            return;
        }

        obsolete
                .stream()
                .filter(path -> workspaces.getWorkspace(Path.of(path)).isPresent())
                .peek(path -> LOG.info("Deleting obsolete workspace at " + path))
                .forEach(workspace -> orchestrator.forcePurgeWorkspaceNoThrows(projectId, Path.of(workspace)));
    }

}
