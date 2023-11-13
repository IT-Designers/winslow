package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.pipeline.ChartDefinition;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.auth.ACL;
import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.*;
import java.util.stream.Stream;


public record PipelineDefinition (
        @Nonnull String id,
        @Nonnull String name,
        @Nullable String description,
        @Nonnull UserInput userInput,
        @Nonnull List<StageDefinition> stages,
        @Nonnull Map<String, String> environment,
        @Nonnull DeletionPolicy deletionPolicy,
        @Nonnull List<Link> groups,
        @Nonnull List<ChartDefinition> charts,
        @Nullable String belongsToProject, // A pipeline can either be shared or owned by a single project
        boolean publicAccess) {

    public PipelineDefinition(@Nonnull String id, @Nonnull String name) {
        this(
                id,
                name,
                null,
                new UserInput(),
                Collections.emptyList(),
                Collections.emptyMap(),
                new DeletionPolicy(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                false
        );
    }

    @ConstructorProperties({
            "id",
            "name",
            "description",
            "requires",
            "stages",
            "requiredEnvVariables",
            "deletionPolicy",
            "groups",
            "charts",
            "belongsToProject",
            "publicAccess",
    })
    public PipelineDefinition(
            // the parameter names must match the corresponding getter names!
            @Nonnull String id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput userInput,
            @Nullable List<StageDefinition> stages,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy,
            @Nullable List<Link> groups,
            @Nullable List<ChartDefinition> charts,
            @Nullable String belongsToProject,
            boolean publicAccess
    ) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of a pipeline must not be blank");
        }

        this.id = id;
        this.name = name;
        this.description = description != null && !description.isBlank() ? description.trim() : null;
        this.userInput = userInput != null ? userInput : new UserInput();
        this.stages = stages != null ? stages : Collections.emptyList();
        this.environment = environment != null ? environment : Collections.emptyMap();
        this.deletionPolicy = deletionPolicy != null ? deletionPolicy : new DeletionPolicy();
        this.groups = groups != null ? groups : Collections.emptyList();
        this.charts = charts != null ? charts : Collections.emptyList();
        this.belongsToProject = belongsToProject;
        this.publicAccess = publicAccess;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(id, "The id of a pipeline must be set");
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        Objects.requireNonNull(userInput, "The user input of a pipeline must be set");
        Objects.requireNonNull(stages, "The stages of a pipeline must be set");
        Objects.requireNonNull(environment, "The environment of a pipeline must be set");
        Objects.requireNonNull(deletionPolicy, "The deletion policy of a pipeline must be set");
        Stream.ofNullable(this.stages).flatMap(List::stream).forEach(StageDefinition::check);
    }

    @Nonnull
    @Transient
    public Optional<String> optDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * @param user The {@link User} to check for.
     * @return Whether the given {@link User} is allowed to change this {@link PipelineDefinition} and its
     * {@link #groups()} associations. This also inherits all privileges of {@link #canBeAccessedBy(User)}.
     */
    public boolean canBeManagedBy(@Nonnull User user) {
        return ACL.canUserManage(user, groups());
    }

    /**
     * @param user The {@link User} to check for.
     * @return Whether the given {@link User} is allowed to see, access and use this {@link PipelineDefinition}.
     */
    public boolean canBeAccessedBy(@Nonnull User user) {
        return publicAccess || ACL.canUserAccess(user, groups());
    }

    @Nonnull
    public PipelineDefinition withoutGroup(@Nonnull String groupName) {
        return new PipelineDefinition(
                id(),
                name(),
                description(),
                userInput(),
                stages(),
                environment(),
                deletionPolicy(),
                groups()
                        .stream()
                        .filter(link -> !Objects.equals(link.name(), groupName))
                        .toList(),
                charts(),
                belongsToProject(),
                publicAccess()
        );
    }

    @Nonnull
    public PipelineDefinition withUserAndRole(@Nonnull String group, @Nonnull Role role) {
        return new PipelineDefinition(
                id(),
                name(),
                description(),
                userInput(),
                stages(),
                environment(),
                deletionPolicy(),
                Stream.concat(
                        Stream.of(new Link(group, role)),
                        groups()
                                .stream()
                                .filter(link -> !Objects.equals(link.name(), group))
                ).toList(),
                charts(),
                belongsToProject(),
                publicAccess()
        );
    }

    @Nonnull
    public PipelineDefinition withAssignedProject(@Nullable String projectId) {
        return new PipelineDefinition(
                id(),
                name(),
                description(),
                userInput(),
                stages(),
                environment(),
                deletionPolicy(),
                groups(),
                charts(),
                projectId,
                publicAccess()
        );
    }

    public boolean isAvailableForProject(@Nonnull String projectId) {
        if (belongsToProject() == null) {
            return true;
        }
        return belongsToProject().equals(projectId);
    }
}
