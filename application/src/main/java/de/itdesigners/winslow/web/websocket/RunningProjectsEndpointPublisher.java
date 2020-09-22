package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.LogEntryInfo;
import de.itdesigners.winslow.api.pipeline.Stats;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.itdesigners.winslow.web.websocket.ProjectsEndpointController.*;

public class RunningProjectsEndpointPublisher implements Pollable {

    private final @Nonnull Map<String, LogFileInfo> logFileSize = new HashMap<>();

    private final @Nonnull MessageSender sender;
    private final @Nonnull Winslow       winslow;

    private @Nonnull Project project;


    public RunningProjectsEndpointPublisher(
            @Nonnull MessageSender sender,
            @Nonnull Winslow winslow,
            @Nonnull Project project) {
        this.sender  = sender;
        this.winslow = winslow;
        this.project = project;
    }

    public void updateProject(@Nonnull Project project) {
        this.project = project;
    }


    private void publishProjectUpdate(@Nonnull String topic, @Nullable Object value) {
        this.sender.publishProjectUpdate(winslow, topic, project.getId(), value, project);
    }

    private void publishUpdate(@Nullable Stats stats) {
        this.publishProjectUpdate(String.format(TOPIC_PROJECT_SPECIFIC_STATS, project.getId()), stats);
    }

    private void publishUpdate(@Nullable Collection<LogEntryInfo> entries) {
        this.publishProjectUpdate(String.format(TOPIC_PROJECT_SPECIFIC_LOGS_LATEST, project.getId()), entries);
    }

    private void publishUpdate(@Nullable Collection<LogEntryInfo> entries, @Nonnull String stageId) {
        this.publishProjectUpdate(String.format(TOPIC_PROJECT_SPECIFIC_LOGS_STAGE, project.getId(), stageId), entries);
    }

    @Override
    public void poll() {
        winslow.getOrchestrator().getRunningStageStats(project).ifPresent(this::publishUpdate);
        winslow.getOrchestrator()
               .getPipeline(project)
               .flatMap(Pipeline::getActiveExecutionGroup)
               .stream()
               .flatMap(ExecutionGroup::getStages)
               .sequential()
               .reduce((first, second) -> second)
               .ifPresent(stage -> this.getLogEntryLatestAfterHead(stage).ifPresent(logs -> {
                   publishUpdate(logs);
                   publishUpdate(logs, stage.getFullyQualifiedId());
               }));

    }

    private synchronized Optional<List<LogEntryInfo>> getLogEntryLatestAfterHead(@Nonnull Stage stage) {
        var stageId = stage.getId().getFullyQualified();
        var info = this.logFileSize.computeIfAbsent(
                stage.getProjectRelativeId(),
                id -> new LogFileInfo()
        );

        if (info.checkIfLargerAndUpdate(winslow.getOrchestrator().getLogSize(project, stageId))) {
            return Optional
                    .of(winslow
                                .getOrchestrator()
                                .getLogs(project, stage.getId())
                                .sequential()
                                .skip(info.lines)
                                .map(e -> new LogEntryInfo(info.lines++, stageId, e))
                                .collect(Collectors.toList())
                    )
                    .filter(l -> !l.isEmpty());
        } else {
            return Optional.empty();
        }
    }

    public synchronized List<LogEntryInfo> getLogEntryLatestUpToHead() {
        return getLogEntryLatestUpToHead(
                winslow,
                project.getId(),
                id -> Optional
                        .ofNullable(this.logFileSize.get(id.getFullyQualified()))
                        .map(i -> i.lines)
                        .orElse(Long.MAX_VALUE)
        );
    }

    public synchronized List<LogEntryInfo> getLogEntryUpToHead(@Nonnull String stageId) {
        return getLogEntryUpToHead(
                winslow,
                project.getId(),
                id -> Optional
                        .ofNullable(this.logFileSize.get(id.getFullyQualified()))
                        .map(i -> i.lines)
                        .orElse(Long.MAX_VALUE),
                stageId
        );
    }

    public static List<LogEntryInfo> getLogEntryLatestUpToHead(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback) {
        return winslow
                .getOrchestrator()
                .getPipeline(projectId)
                .flatMap(pipeline -> pipeline
                        .getActiveOrPreviousExecutionGroup()
                        .stream()
                        .flatMap(ExecutionGroup::getStages)
                        .sequential()
                        .reduce((first, second) -> second)
                        .map(stage -> loadLogs(winslow, projectId, logFileLimitCallback, stage))
                )
                .orElseGet(Collections::emptyList);
    }

    public static List<LogEntryInfo> getLogEntryUpToHead(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback,
            @Nonnull String stageId) {
        return winslow
                .getOrchestrator()
                .getPipeline(projectId)
                .flatMap(pipeline -> pipeline
                        .getActiveAndPastExecutionGroups()
                        .flatMap(ExecutionGroup::getStages)
                        .filter(stage -> stage.getFullyQualifiedId().equals(stageId))
                        .findAny()
                        .map(stage -> loadLogs(winslow, projectId, logFileLimitCallback, stage))
                )
                .orElseGet(Collections::emptyList);
    }

    @Nonnull
    private static List<LogEntryInfo> loadLogs(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback, Stage stage) {
        var line  = new AtomicLong(0L);
        var limit = logFileLimitCallback.apply(stage.getId());
        return winslow
                .getOrchestrator()
                .getLogs(projectId, stage.getFullyQualifiedId())
                .sequential()
                .limit(limit)
                .map(e -> new LogEntryInfo(
                        line.getAndIncrement(),
                        stage.getFullyQualifiedId(),
                        e
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        this.publishUpdate((Stats) null);
    }

    private static class LogFileInfo {
        public long fileSize;
        public long lines;

        public boolean checkIfLargerAndUpdate(long fileSize) {
            var larger = fileSize > this.fileSize;
            this.fileSize = fileSize;
            return larger;
        }
    }
}
