package de.itd.tracking.winslow.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Pipeline {
    private final String      name;
    private final String      desc;
    private final UserInput   userInput;
    private final List<Stage> stages;

    public Pipeline(String name, String desc, UserInput userInput, List<Stage> stages) {
        this.name = name;
        this.desc = desc;
        this.userInput = userInput;
        this.stages = stages;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a pipeline must be set");
        this.stages.forEach(Stage::check);
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

    public List<Stage> getStages() {
        return stages != null ? Collections.unmodifiableList(stages) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "',desc='" + this.desc + "',userInput=" + this.userInput + ",stages=" + this.stages + "}#" + this.hashCode();
    }
}
