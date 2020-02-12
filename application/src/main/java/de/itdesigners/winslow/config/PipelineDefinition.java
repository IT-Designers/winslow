package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.project.DeletionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class PipelineDefinition {

    private final @Nonnull  String                name;
    private final @Nullable String                desc;
    private final @Nullable UserInput             userInput;
    private final @Nullable List<StageDefinition> stages;
    private final @Nullable Map<String, String>   env;
    private final @Nullable DeletionPolicy        deletionPolicy;
    private final @Nullable List<String>          markers;


    public PipelineDefinition( // the parameter names must match the corresponding getter names!
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput requires,
            // null-able for backwards compatibility
            @Nullable List<StageDefinition> stages,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy,
            // null-able for backwards compatibility
            @Nullable List<String> markers) {
        this.name           = name;
        this.desc           = description;
        this.userInput      = requires;
        this.stages         = stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
        this.env            = environment != null ? Collections.unmodifiableMap(environment) : Collections.emptyMap();
        this.deletionPolicy = deletionPolicy;
        this.markers        = markers != null ? Collections.unmodifiableList(markers) : Collections.emptyList();
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        Stream.ofNullable(this.stages).flatMap(List::stream).forEach(StageDefinition::check);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Optional<String> getDescription() {
        return Optional.ofNullable(desc);
    }

    @Nonnull
    public Optional<UserInput> getRequires() {
        return Optional.ofNullable(userInput);
    }

    @Nonnull
    public List<StageDefinition> getStages() {
        return stages != null ? stages : Collections.emptyList();
    }

    @Nonnull
    public Map<String, String> getEnvironment() {
        return env != null ? env : Collections.emptyMap();
    }

    @Nonnull
    public Optional<DeletionPolicy> getDeletionPolicy() {
        return Optional.ofNullable(this.deletionPolicy);
    }

    @Nonnull
    public List<String> getMarkers() {
        return markers != null ? markers : Collections.emptyList();
    }

    @Nonnull
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "@{name='" + this.name
                + "',desc='" + this.desc
                + "',userInput=" + this.userInput
                + ",stages=" + this.stages
                + ",env=" + this.env
                + ",deletionPolicy=" + this.deletionPolicy
                + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PipelineDefinition pipelineDefinition = (PipelineDefinition) o;
        return Objects.equals(name, pipelineDefinition.name)
                && Objects.equals(desc, pipelineDefinition.desc)
                && Objects.equals(userInput, pipelineDefinition.userInput)
                && Objects.equals(stages, pipelineDefinition.stages)
                && Objects.equals(env, pipelineDefinition.env)
                && Objects.equals(deletionPolicy, pipelineDefinition.deletionPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, userInput, stages, env);
    }
}
