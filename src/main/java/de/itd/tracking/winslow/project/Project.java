package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.auth.Group;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.Pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Project {

    private final UUID     id;
    private final Pipeline pipeline;

    private final User        owner;
    private final List<Group> groups = new ArrayList<>();

    private final List<UUID> stageHistory = new ArrayList<>();

    private int nextStage = 0;

    public Project(UUID id, Pipeline pipeline, User owner) {
        this.id = id;
        this.pipeline = pipeline;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public User getOwner() {
        return owner;
    }

    public Iterable<Group> getGroups() {
        return Collections.unmodifiableList(this.groups);
    }

    public void addGroup(Group group) {
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public boolean removeGroup(Group group) {
        return this.groups.remove(group);
    }

    public Iterable<UUID> getStageHistory() {
        return Collections.unmodifiableList(this.stageHistory);
    }
}
