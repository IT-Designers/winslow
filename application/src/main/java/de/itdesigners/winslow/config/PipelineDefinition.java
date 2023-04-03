package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.auth.ACL;
import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.*;
import java.util.stream.Stream;


public record PipelineDefinition(
        @Nonnull String name,
        @Nullable String description,
        @Nonnull UserInput userInput,
        @Nonnull List<StageDefinition> stages,
        @Nonnull Map<String, String> environment,
        @Nonnull DeletionPolicy deletionPolicy,
        @Nonnull List<String> markers,
        @Nonnull List<Link> groups,
        boolean publicAccess) {

    public PipelineDefinition(@Nonnull String name) {
        this(
                name,
                null,
                new UserInput(),
                Collections.emptyList(),
                Collections.emptyMap(),
                new DeletionPolicy(),
                Collections.emptyList(),
                Collections.emptyList(),
                false
        );
    }

    @ConstructorProperties({
            "name",
            "description",
            "requires",
            "stages",
            "requiredEnvVariables",
            "deletionPolicy",
            "markers",
            "groups",
            "publicAccess"
    })
    public PipelineDefinition( // the parameter names must match the corresponding getter names!
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput userInput,
            @Nullable List<StageDefinition> stages,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy,
            @Nullable List<String> markers,
            @Nullable List<Link> groups,
            boolean publicAccess
    ) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of a pipeline must not be blank");
        }

        this.name           = name;
        this.description    = description != null && !description.isBlank() ? description.trim() : null;
        this.userInput      = userInput != null ? userInput : new UserInput();
        this.stages         = stages != null ? stages : Collections.emptyList();
        this.environment    = environment != null ? environment : Collections.emptyMap();
        this.deletionPolicy = deletionPolicy != null ? deletionPolicy : new DeletionPolicy();
        this.markers        = markers != null ? markers : Collections.emptyList();
        this.groups         = groups != null ? groups : Collections.emptyList();
        this.publicAccess   = publicAccess;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        Objects.requireNonNull(userInput, "The user input of a pipeline must be set");
        Objects.requireNonNull(stages, "The stages of a pipeline must be set");
        Objects.requireNonNull(environment, "The environment of a pipeline must be set");
        Objects.requireNonNull(deletionPolicy, "The deletion policy of a pipeline must be set");
        Objects.requireNonNull(markers, "The markers of a pipeline must be set");
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

    public PipelineDefinition withUserAndRole(@Nonnull String user, @Nonnull Role role) {
        return new PipelineDefinition(
                name(),
                description(),
                userInput(),
                stages(),
                environment(),
                deletionPolicy(),
                markers(),
                Stream.concat(
                        Stream.of(new Link(user, role)),
                        groups()
                                .stream()
                                .filter(link -> !Objects.equals(link.name(), user))
                ).toList(),
                publicAccess()
        );
    }
}
