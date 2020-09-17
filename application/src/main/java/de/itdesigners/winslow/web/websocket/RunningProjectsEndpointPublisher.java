package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.Stats;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static de.itdesigners.winslow.web.websocket.ProjectsEndpointController.TOPIC_PROJECT_SPECIFIC_STATS;

public class RunningProjectsEndpointPublisher implements Pollable {

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

    @Override
    public void poll() {
        var stats = winslow.getOrchestrator().getRunningStageStats(project);

        stats.ifPresent(this::publishUpdate);


    }

    @Override
    public void close() {
        this.publishUpdate((Stats) null);
    }
}
