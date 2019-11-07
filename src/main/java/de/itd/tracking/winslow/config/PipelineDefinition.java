package de.itd.tracking.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class PipelineDefinition {
    @Nonnull private final  String                name;
    @Nullable private final String                desc;
    @Nullable private final UserInput             userInput;
    @Nullable private final List<StageDefinition> stages;

    public PipelineDefinition(
            @Nonnull String name,
            @Nullable String description,
            @Nullable UserInput userInput,
            @Nonnull List<StageDefinition> stageDefinitions) {
        this.name      = name;
        this.desc      = description;
        this.userInput = userInput;
        this.stages    = stageDefinitions;
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
    public Optional<UserInput> getUserInput() {
        return Optional.ofNullable(userInput);
    }

    @Nonnull
    public List<StageDefinition> getStages() {
        return stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
    }

    @Nonnull
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "@{name='" + this.name + "',desc='" + this.desc + "',userInput=" + this.userInput + ",stages=" + this.stages + "}#"
                + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PipelineDefinition pipelineDefinition = (PipelineDefinition) o;
        return Objects.equals(name, pipelineDefinition.name) && Objects.equals(desc, pipelineDefinition.desc) && Objects
                .equals(userInput, pipelineDefinition.userInput) && Objects.equals(stages, pipelineDefinition.stages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, userInput, stages);
    }
}
