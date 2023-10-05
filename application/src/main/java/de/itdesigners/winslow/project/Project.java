package de.itdesigners.winslow.project;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.auth.ACL;
import de.itdesigners.winslow.auth.Prefix;
import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;

public class Project {

    private final @Nonnull String     id;
    private final @Nonnull String     accountingGroup;
    private final @Nonnull List<Link> groups;

    private @Nullable List<String> tags;

    private @Nonnull  String             name;
    private @Nonnull String             pipelineDefinitionId;
    private           boolean            publicAccess;
    private @Nullable ResourceLimitation resourceLimit;

    Project(@Nonnull String id, @Nonnull User user, @Nonnull String pipelineDefinitionId) {
        this.id                   = id;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.name                 = "[no name]";
        this.groups               = new ArrayList<>();
        this.accountingGroup      = Prefix.User.wrap(user.name());

        this.groups.add(new Link(
                this.accountingGroup,
                Role.OWNER
        ));
    }

    @ConstructorProperties({
            "id",
            "accountingGroup",
            "owner",
            "groups",
            "tags",
            "name",
            "public",
            "pipelineDefinition",
            "resourceLimit"
    })
    public Project(
            @Nonnull String id,
            @Nonnull String accountingGroup,
            @Nullable Iterable<Link> groups,
            @Nullable Iterable<String> tags,
            @Nonnull String name,
            @Nullable Boolean publicAccess,
            @Nonnull String pipelineDefinitionId,
            @Nullable ResourceLimitation resourceLimit) {
        this.id                   = id;
        this.accountingGroup      = accountingGroup;
        this.groups               = new ArrayList<>();
        this.tags                 = null;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.name                 = name;
        this.publicAccess         = Objects.requireNonNullElse(publicAccess, false);
        this.resourceLimit        = resourceLimit;

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

    /**
     * @return The name of the {@link de.itdesigners.winslow.auth.Group} to book the resource usage on
     */
    @Nonnull
    public String getAccountingGroup() {
        return accountingGroup;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public boolean isPublic() {
        return publicAccess;
    }

    public void setPublic(boolean publicAccessible) {
        this.publicAccess = publicAccessible;
    }

    public void setPipelineDefinitionId(@Nonnull String pipelineDefinitionId) {
        this.pipelineDefinitionId = pipelineDefinitionId;
    }

    @Nonnull
    public String getPipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    @Nonnull
    public List<Link> getGroups() {
        return Collections.unmodifiableList(this.groups);
    }

    public void addGroup(@Nonnull Link group) throws IllegalArgumentException {
        var numberOfOwners = this.groups.stream().filter(l -> Role.OWNER == l.role()).count();
        for (int i = 0; i < this.groups.size(); ++i) {
            if (this.groups.get(i).name().equals(group.name())) {
                if (numberOfOwners < 2 && this.groups.get(i).role() == Role.OWNER && group.role() != Role.OWNER) {
                    throw new IllegalArgumentException("Cannot remove last remaining owner");
                }
                this.groups.set(i, group);
                return;
            }
        }
        this.groups.add(group);
    }

    public boolean removeGroup(@Nonnull String groupName) throws IllegalArgumentException {
        var numberOfOwners = this.groups.stream().filter(l -> Role.OWNER == l.role()).count();
        for (int i = 0; i < this.groups.size(); ++i) {
            if (this.groups.get(i).name().equals(groupName)) {
                if (numberOfOwners < 2 && this.groups.get(i).role() == Role.OWNER) {
                    throw new IllegalArgumentException("Cannot remove last remaining owner");
                }
                this.groups.remove(i);
                return true;
            }
        }
        return false;
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

    /**
     * @param user The {@link User} to check for
     * @return Whether the given {@link User} has at least the {@link Role#OWNER} privileges
     */
    public boolean canBeManagedBy(@Nonnull User user) {
        return ACL.canUserManage(user, getGroups());
    }

    /**
     * @param user The {@link User} to check for
     * @return Whether the given {@link User} has at least the {@link Role#MAINTAINER} privileges
     */
    public boolean canBeMaintainedBy(@Nonnull User user) {
        return ACL.canUserMaintain(user, getGroups());
    }

    /**
     * @param user The {@link User} to check for
     * @return Whether the given {@link User} has at least the {@link Role#MEMBER} privileges. Always returns true if
     * this {@link Project} {@link #isPublic()}
     */
    public boolean canBeAccessedBy(@Nonnull User user) {
        return publicAccess || ACL.canUserAccess(user, getGroups());
    }

    @Nonnull
    public Optional<ResourceLimitation> getResourceLimitation() {
        return Optional.ofNullable(this.resourceLimit);
    }

    public void setResourceLimitation(@Nullable ResourceLimitation limit) {
        this.resourceLimit = limit;
    }
}
