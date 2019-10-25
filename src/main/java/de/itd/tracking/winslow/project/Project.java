package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Project {

    @Nonnull private final String id;
    @Nonnull private final String owner;

    @Nullable private List<String> groups = new ArrayList<>();
    @Nullable private List<String> tags   = new ArrayList<>();

    @Nonnull private PipelineDefinition pipeline;
    @Nonnull private String             name;

    public Project(@Nonnull String id, String owner, @Nonnull PipelineDefinition pipeline) {
        this(id, owner, pipeline, "");
    }

    public Project(@Nonnull String id, String owner, @Nonnull PipelineDefinition pipeline, @Nonnull String name) {
        this.id       = id;
        this.owner    = owner;
        this.pipeline = pipeline;
        this.name     = name;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    @Nonnull
    public String getOwner() {
        return owner;
    }

    public void setPipelineDefinition(@Nonnull PipelineDefinition definition) {
        Objects.requireNonNull(definition);
        this.pipeline = definition;
    }

    @Nonnull
    public PipelineDefinition getPipelineDefinition() {
        return pipeline;
    }

    @Nonnull
    public Iterable<String> getGroups() {
        if (this.groups != null) {
            return Collections.unmodifiableList(this.groups);
        } else {
            return Collections.emptyList();
        }
    }

    public void addGroup(String group) {
        if (this.groups == null) {
            this.groups = new ArrayList<>();
        }
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public boolean removeGroup(String group) {
        return this.groups != null && this.groups.remove(group);
    }

    @Nonnull
    public Iterable<String> getTags() {
        if (this.tags != null) {
            return Collections.unmodifiableList(this.tags);
        } else {
            return Collections.emptyList();
        }
    }

    public void addTag(@Nonnull String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public boolean removeTag(@Nonnull String tag) {
        return this.tags != null && this.tags.remove(tag);
    }

    public void setTags(@Nullable String...tags) {
        if (this.tags == null) {
            this.tags = new ArrayList<>(tags != null ? tags.length : 0);
        } else {
            this.tags.clear();
        }
        if (tags != null) {
            this.tags.addAll(Arrays.asList(tags));
        }
    }
}
