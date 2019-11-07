package de.itd.tracking.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class StageDefinition {
    private final @Nonnull  String              name;
    private final @Nullable String              desc;
    private final @Nullable Image               image;
    private final @Nullable Requirements        requires;
    private final @Nullable UserInput           userInput;
    private final @Nullable Map<String, String> env;
    private final @Nullable Highlight           highlight;

    public StageDefinition(
            @Nonnull String name,
            @Nullable String description,
            @Nullable Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight) {
        this.name      = name;
        this.desc      = description;
        this.image     = image;
        this.requires  = requirements;
        this.userInput = requires;
        this.env       = environment;
        this.highlight = highlight;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
    }

    @Nonnull
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

    public Optional<UserInput> getRequires() {
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
        return Objects.equals(name, stageDefinition.name) && Objects.equals(
                desc,
                stageDefinition.desc
        ) && Objects.equals(
                image,
                stageDefinition.image
        ) && Objects.equals(
                requires,
                stageDefinition.requires
        ) && Objects
                .equals(userInput, stageDefinition.userInput) && Objects.equals(
                env,
                stageDefinition.env
        ) && Objects.equals(
                highlight,
                stageDefinition.highlight
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, image, requires, userInput, env, highlight);
    }
}
