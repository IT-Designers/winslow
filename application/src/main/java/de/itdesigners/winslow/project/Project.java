package de.itdesigners.winslow.project;

import de.itdesigners.winslow.LockedContainer;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;
import java.util.logging.Logger;

public class Project {

    private static final Logger LOG = Logger.getLogger(LockedContainer.class.getSimpleName());

    private @Nonnull final String id;
    private @Nonnull final String owner;

    private @Nullable List<String> groups;
    private @Nullable List<String> tags;

    private @Nonnull  PipelineDefinition pipeline;
    private @Nonnull  String             name;
    private           boolean            publicAccess;
    private @Nullable ResourceLimitation resourceLimit;

    Project(@Nonnull String id, @Nonnull String owner, @Nonnull PipelineDefinition pipeline) {
        this.id       = id;
        this.owner    = owner;
        this.pipeline = pipeline;
        this.name     = "[no name]";
    }

    @ConstructorProperties({"id", "owner", "groups", "tags", "name", "public", "pipelineDefinition", "resourceLimit"})
    public Project(
            @Nonnull String id,
            @Nonnull String owner,
            @Nullable Iterable<String> groups,
            @Nullable Iterable<String> tags,
            @Nonnull String name,
            @Nullable Boolean publicAccess,
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nullable ResourceLimitation resourceLimit) {
        this.id            = id;
        this.owner         = owner;
        this.groups        = null;
        this.tags          = null;
        this.pipeline      = pipelineDefinition;
        this.name          = name;
        this.publicAccess  = Objects.requireNonNullElse(publicAccess, false);
        this.resourceLimit = resourceLimit;

        if (groups != null) {
            groups.forEach(this::addGroup);
        }

        if (tags != null) {
            tags.forEach(this::addTag);
        }

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

    public boolean isPublic() {
        return publicAccess;
    }

    public void setPublic(boolean publicAccessible) {
        this.publicAccess = publicAccessible;
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
    public List<String> getGroups() {
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
    public List<String> getTags() {
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

    public void setTags(@Nullable String... tags) {
        if (this.tags == null) {
            this.tags = new ArrayList<>(tags != null ? tags.length : 0);
        } else {
            this.tags.clear();
        }
        if (tags != null) {
            this.tags.addAll(Arrays.asList(tags));
        }
    }

    public boolean canBeManagedBy(@Nonnull User user) {
        return user.hasSuperPrivileges() || getOwner().equals(user.name());
    }

    public boolean canBeAccessedBy(@Nonnull User user) {
        return this.isPublic()
                || this.canBeManagedBy(user)
                || (this.groups != null && user.getGroups().stream().anyMatch(g -> this.groups.contains(g.name())));
    }

    @Nonnull
    public Optional<ResourceLimitation> getResourceLimitation() {
        return Optional.ofNullable(this.resourceLimit);
    }

    public void setResourceLimitation(@Nullable ResourceLimitation limit) {
        this.resourceLimit = limit;
    }
}
