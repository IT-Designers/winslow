package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Project {

    private final String   id;
    private final Pipeline pipeline;
    private final String   owner;
    private final List<String> groups = new ArrayList<>();

    private String name;
    private int nextStage = 0;

    public Project(String id, Pipeline pipeline, String owner) {
        this.id = id;
        this.pipeline = pipeline;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNextStageIndex() {
        return nextStage;
    }

    public void setNextStageIndex(int index) {
        this.nextStage = index;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public String getOwner() {
        return owner;
    }

    public Iterable<String> getGroups() {
        return Collections.unmodifiableList(this.groups);
    }

    public void addGroup(String group) {
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public boolean removeGroup(String group) {
        return this.groups.remove(group);
    }

    public Iterable<String> getStages() {
        return getPipeline()
                .getStages()
                .stream()
                .map(Stage::getName)
                .collect(Collectors.toUnmodifiableList());
    }
}
