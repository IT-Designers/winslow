package de.itdesigners.winslow.config;

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
    private final           boolean             discardable;
    private final           boolean             privileged;

    public StageDefinition(
            @Nonnull String name,
            @Nullable String description,
            @Nullable Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight,
            // null-able for backwards compatibility
            @Nullable Boolean discardable,
            @Nullable Boolean privileged) {
        this.name        = name;
        this.desc        = description;
        this.image       = image;
        this.requires    = requirements;
        this.userInput   = requires;
        this.env         = environment;
        this.highlight   = highlight;
        this.discardable = discardable != null && discardable;
        this.privileged  = privileged != null && privileged;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
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
    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }

    @Nonnull
    public Optional<Requirements> getRequirements() {
        return Optional.ofNullable(requires);
    }

    @Nonnull
    public Optional<UserInput> getRequires() {
        return Optional.ofNullable(userInput);
    }

    @Nonnull
    public Map<String, String> getEnvironment() {
        return env != null ? env : Collections.emptyMap();
    }

    @Nonnull
    public Optional<Highlight> getHighlight() {
        return Optional.ofNullable(highlight);
    }

    /**
     * @return Whether associated resources (like the workspace) that were used when executing this
     * stage are allowed to be discarded as soon as the next stage succeeded in execution
     */
    public boolean isDiscardable() {
        return discardable;
    }

    /**
     * @return Whether this stages requires to be executed in a privileged environment
     */
    public boolean isPrivileged() {
        return privileged;
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
        return Objects.equals(
                name,
                stageDefinition.name
        ) && Objects.equals(
                desc,
                stageDefinition.desc
        ) && Objects.equals(
                image,
                stageDefinition.image
        ) && Objects.equals(
                requires,
                stageDefinition.requires
        ) && Objects.equals(
                userInput,
                stageDefinition.userInput
        ) && Objects.equals(
                env,
                stageDefinition.env
        ) && Objects.equals(
                highlight,
                stageDefinition.highlight
        ) && Objects.equals(
                this.discardable,
                stageDefinition.discardable
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, image, requires, userInput, env, highlight, this.discardable);
    }
}
