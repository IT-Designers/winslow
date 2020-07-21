package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StageTimeoutDetection implements PipelineUpdater.NoAccessUpdater, PipelineUpdater.ExclusiveAccessUpdater {

    private final @Nonnull List<String> stagesToCheck;

    public StageTimeoutDetection(@Nonnull List<String> stagesToCheck) {
        this.stagesToCheck = stagesToCheck;
    }

    public static Optional<StageTimeoutDetection> check(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        var stages = Stream
                .ofNullable(pipelineReadOnly)
                .flatMap(pipeline -> pipeline
                        .getActiveExecutionGroup()
                        .stream()
                        .flatMap(ExecutionGroup::getRunningStages)
                        .map(Stage::getFullyQualifiedId))
                .filter(stageId -> !orchestrator.getLogRepository().isLocked(projectId, stageId))
                .collect(Collectors.toList());
        if (stages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new StageTimeoutDetection(stages));
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
                    .getActiveExecutionGroup()
                    .stream()
                    .flatMap(ExecutionGroup::getRunningStages)
                    .filter(stage -> stagesToCheck.contains(stage.getFullyQualifiedId()))
                    .filter(stage -> !orchestrator.getLogRepository().isLocked(projectId, stage.getFullyQualifiedId()))
                    .peek(stage -> stage.finishNow(State.Failed))
                    .count();

            if (changes > 0) {
                return pipeline;
            }
        }
        return null;
    }
}
