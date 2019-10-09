package de.itd.tracking.winslow.config;

import java.util.*;

public class StageDefinition {
    private final String              name;
    private final String              desc;
    private final Image               image;
    private final Requirements        requires;
    private final UserInput           userInput;
    private final Map<String, String> env;
    private final Highlight           highlight;

    public StageDefinition(
            String name,
            String desc,
            Image image,
            Requirements requires,
            UserInput userInput,
            HashMap<String, String> env,
            Highlight highlight) {
        this.name = name;
        this.desc = desc;
        this.image = image;
        this.requires = requires;
        this.userInput = userInput;
        this.env = env;
        this.highlight = highlight;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(desc);
    }

    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }

    public Optional<Requirements> getRequirements() {
        return Optional.ofNullable(requires);
    }

    public Optional<UserInput> getUserInput() {
        return Optional.ofNullable(userInput);
    }

    public Map<String, String> getEnvironment() {
        return env != null ? env : Collections.emptyMap();
    }

    public Optional<Highlight> getHighlight() {
        return Optional.ofNullable(highlight);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "',desc='" + this.desc + "',image=" + this.image + ",userInput=" + this.userInput + "}#" + this
                .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StageDefinition stageDefinition = (StageDefinition) o;
        return Objects.equals(name, stageDefinition.name) && Objects.equals(desc,
                                                                            stageDefinition.desc
                                                                           ) && Objects.equals(image,
                                                                                               stageDefinition.image
                                                                                              ) && Objects.equals(
                requires,
                stageDefinition.requires
                                                                                                                 ) && Objects
                .equals(userInput, stageDefinition.userInput) && Objects.equals(env,
                                                                                stageDefinition.env
                                                                               ) && Objects.equals(highlight,
                                                                                                   stageDefinition.highlight
                                                                                                  );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, image, requires, userInput, env, highlight);
    }
}
