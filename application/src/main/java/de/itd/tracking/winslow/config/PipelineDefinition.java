package de.itd.tracking.winslow.config;

import de.itd.tracking.winslow.pipeline.DeletionPolicy;

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

    public PipelineDefinition(
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput requires,
            @Nonnull List<StageDefinition> stageDefinitions,
            @Nullable Map<String, String> environment,
            @Nullable DeletionPolicy deletionPolicy) {
        this.name           = name;
        this.desc           = description;
        this.userInput      = requires;
        this.stages         = stageDefinitions;
        this.env            = environment;
        this.deletionPolicy = deletionPolicy;
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
        return stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
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
