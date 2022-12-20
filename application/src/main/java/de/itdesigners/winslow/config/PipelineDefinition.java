package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;

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
        @Nonnull List<String> markers) {

    public PipelineDefinition(@Nonnull String name) {
        this(
                name,
                null,
                new UserInput(),
                Collections.emptyList(),
                Collections.emptyMap(),
                new DeletionPolicy(),
                Collections.emptyList()
        );
    }

    @ConstructorProperties({"name", "description", "requires", "stages", "requiredEnvVariables", "deletionPolicy", "markers"})
    public PipelineDefinition( // the parameter names must match the corresponding getter names!
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput userInput,
            @Nullable List<StageDefinition> stages,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy,
            @Nullable List<String> markers
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
}
