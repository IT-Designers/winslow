package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;
import java.util.stream.Stream;


public record PipelineDefinition(
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull UserInput userInput,
        @Nonnull List<StageDefinition> stages,
        @Nonnull Map<String, String> environment,
        @Nonnull DeletionPolicy deletionPolicy,
        @Nonnull List<String> markers) {


    @ConstructorProperties({"name", "description", "requires", "stages", "requiredEnvVariables", "deletionPolicy", "markers"})
    public PipelineDefinition( // the parameter names must match the corresponding getter names!
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput userInput,
            // null-able for backwards compatibility
            @Nullable List<StageDefinition> stages,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy,
            // null-able for backwards compatibility
            @Nullable List<String> markers
    ) {
        this.name        = name;
        this.description = description != null ? description : "";
        this.userInput   = userInput != null ? userInput : new UserInput(null, null);
        ;
        this.stages         = stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
        this.environment    = environment != null ? Collections.unmodifiableMap(environment) : Collections.emptyMap();
        this.deletionPolicy = deletionPolicy;
        this.markers        = markers != null ? Collections.unmodifiableList(markers) : Collections.emptyList();
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        Stream.ofNullable(this.stages).flatMap(List::stream).forEach(StageDefinition::check);
    }

    @Nonnull
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "@{name='" + this.name
                + "',description='" + this.description
                + "',userInput=" + this.userInput
                + ",stages=" + this.stages
                + ",env=" + this.environment
                + ",deletionPolicy=" + this.deletionPolicy
                + "}#" + this.hashCode();
    }


}
