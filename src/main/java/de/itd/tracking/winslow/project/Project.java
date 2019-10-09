package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project {

    @Nonnull private final String             id;
    @Nonnull private final String             owner;
    @Nonnull private final List<String>       groups = new ArrayList<>();
    @Nonnull private final PipelineDefinition pipeline;

    private String name;

    public Project(@Nonnull String id, String owner, @Nonnull PipelineDefinition pipeline) {
        this(id, owner, pipeline, "");
    }

    public Project(@Nonnull String id, String owner, @Nonnull PipelineDefinition pipeline, String name) {
        this.id = id;
        this.owner = owner;
        this.pipeline = pipeline;
        this.name = name;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nonnull
    public String getOwner() {
        return owner;
    }

    @Nonnull
    public PipelineDefinition getPipelineDefinition() {
        return pipeline;
    }

    @Nonnull
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
}
