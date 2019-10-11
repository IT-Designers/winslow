package de.itd.tracking.winslow.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PipelineDefinition {
    private final String                name;
    private final String                desc;
    private final UserInput             userInput;
    private final List<StageDefinition> stages;

    public PipelineDefinition(String name, String desc, UserInput userInput, List<StageDefinition> stages) {
        this.name      = name;
        this.desc      = desc;
        this.userInput = userInput;
        this.stages    = stages;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        this.stages.forEach(StageDefinition::check);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(desc);
    }

    public Optional<UserInput> getUserInput() {
        return Optional.ofNullable(userInput);
    }

    public List<StageDefinition> getStageDefinitions() {
        return stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "',desc='" + this.desc + "',userInput=" + this.userInput + ",stages=" + this.stages + "}#" + this
                .hashCode();
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
