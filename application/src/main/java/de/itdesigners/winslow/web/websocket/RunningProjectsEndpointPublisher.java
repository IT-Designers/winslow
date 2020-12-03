package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.LogEntryInfo;
import de.itdesigners.winslow.api.pipeline.StateInfo;
import de.itdesigners.winslow.api.pipeline.Stats;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.api.ProjectsController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static de.itdesigners.winslow.web.websocket.ProjectsEndpointController.*;

public class RunningProjectsEndpointPublisher implements Pollable {

    private final @Nonnull Map<String, LogFileInfo> logFileSize = new HashMap<>();

    private final @Nonnull MessageSender sender;
    private final @Nonnull Winslow       winslow;

    private @Nonnull Project project;

    private @Nullable StateInfo prevStateInfo;


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

    private void publishStateInfoUpdate(@Nullable StateInfo info) {
        if (!Objects.equals(prevStateInfo, info)) {
            this.publishProjectUpdate(TOPIC_PROJECT_STATES, info);
            this.prevStateInfo = info;
        }
    }

    @Override
    public void poll() {
        winslow.getOrchestrator().getRunningStageStats(project).ifPresent(this::publishUpdate);
        winslow.getOrchestrator().getPipeline(project).ifPresent(pipeline -> {
            pipeline
                    .getActiveExecutionGroup()
                    .stream()
                    .flatMap(ExecutionGroup::getStages)
                    .sequential()
                    .map(stage -> this
                            .getLogEntryLatestAfterHead(stage)
                            .map(logs -> {
                                publishUpdate(logs, stage.getFullyQualifiedId());
                                return logs;
                            })
                    )
                    .reduce((first, second) -> second)
                    .flatMap(s -> s)
                    .ifPresent(this::publishUpdate);

            // TODO
            publishStateInfoUpdate(ProjectsController.getStateInfo(winslow, pipeline));
        });


    }

    private synchronized Optional<List<LogEntryInfo>> getLogEntryLatestAfterHead(@Nonnull Stage stage) {
        var stageId = stage.getId().getFullyQualified();
        var info = this.logFileSize.computeIfAbsent(
                stage.getProjectRelativeId(),
                id -> new LogFileInfo()
        );

        var previousSize = info.fileSize;
        if (info.checkIfLargerAndUpdate(winslow.getOrchestrator().getLogSize(project, stageId))) {
            return Optional
                    .of(winslow
                                .getOrchestrator()
                                .getLogs(project, stage.getId(), previousSize)
                                .sequential()
                                .map(e -> new LogEntryInfo(info.lines++, stageId, e))
                                .collect(Collectors.toList())
                    )
                    .filter(l -> !l.isEmpty());
        } else {
            return Optional.empty();
        }
    }

    public synchronized List<LogEntryInfo> getLogEntryLatestUpToHead(@Nullable Integer maxEntries) {
        return getLogEntryLatestUpToHead(
                winslow,
                project.getId(),
                id -> Optional
                        .ofNullable(this.logFileSize.get(id.getFullyQualified()))
                        .map(i -> i.lines)
                        .orElse(Long.MAX_VALUE),
                maxEntries
        );
    }

    public synchronized List<LogEntryInfo> getLogEntryUpToHead(@Nonnull String stageId, @Nullable Integer maxEntries) {
        return getLogEntryUpToHead(
                winslow,
                project.getId(),
                id -> Optional
                        .ofNullable(this.logFileSize.get(id.getFullyQualified()))
                        .map(i -> i.lines)
                        .orElse(Long.MAX_VALUE),
                stageId,
                maxEntries
        );
    }

    public static List<LogEntryInfo> getLogEntryLatestUpToHead(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback,
            @Nullable Integer maxEntries) {
        return winslow
                .getOrchestrator()
                .getPipeline(projectId)
                .flatMap(pipeline -> pipeline
                        .getActiveOrPreviousExecutionGroup()
                        .stream()
                        .flatMap(ExecutionGroup::getStages)
                        .sequential()
                        .reduce((first, second) -> second)
                        .map(stage -> loadLogs(winslow, projectId, logFileLimitCallback, stage, maxEntries))
                )
                .orElseGet(Collections::emptyList);
    }

    public static List<LogEntryInfo> getLogEntryUpToHead(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback,
            @Nonnull String stageId,
            @Nullable Integer maxEntries) {
        return winslow
                .getOrchestrator()
                .getPipeline(projectId)
                .flatMap(pipeline -> pipeline
                        .getActiveAndPastExecutionGroups()
                        .flatMap(ExecutionGroup::getStages)
                        .filter(stage -> stage.getFullyQualifiedId().equals(stageId))
                        .findAny()
                        .map(stage -> loadLogs(winslow, projectId, logFileLimitCallback, stage, maxEntries))
                )
                .orElseGet(Collections::emptyList);
    }

    @Nonnull
    private static List<LogEntryInfo> loadLogs(
            @Nonnull Winslow winslow,
            @Nonnull String projectId,
            @Nonnull Function<StageId, Long> logFileLimitCallback,
            @Nonnull Stage stage,
            @Nullable Integer maxEntries) {
        if (maxEntries != null && maxEntries < 0) {
            return Collections.emptyList();
        }

        var line  = new AtomicLong(0L);
        var limit = logFileLimitCallback.apply(stage.getId());
        var stream = winslow
                .getOrchestrator()
                .getLogs(projectId, stage.getFullyQualifiedId())
                .sequential()
                .limit(limit)
                .map(e -> new LogEntryInfo(
                        line.getAndIncrement(),
                        stage.getFullyQualifiedId(),
                        e
                ));
        if (maxEntries == null || maxEntries == 0) {
            return stream.collect(Collectors.toList());
        } else {
            return new ArrayList<>(
                    stream
                            .sequential()
                            .collect(
                                    Collector.<LogEntryInfo, ArrayDeque<LogEntryInfo>>of(
                                            ArrayDeque::new,
                                            (queue, entry) -> {
                                                while (queue.size() >= maxEntries) {
                                                    queue.pollFirst();
                                                }
                                                queue.add(entry);
                                            },
                                            (acc1, acc2) -> {
                                                while (!acc1.isEmpty()) {
                                                    acc2.addFirst(acc1.pollLast());
                                                }
                                                return acc2;
                                            }
                                    )
                            )
            );
        }
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
