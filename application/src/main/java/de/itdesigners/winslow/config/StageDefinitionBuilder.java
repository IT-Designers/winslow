package de.itdesigners.winslow.config;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StageDefinitionBuilder {

    private @Nullable StageDefinition               template;
    private @Nullable StageDefinition               base;
    private @Nullable Optional<String>              name;
    private @Nullable Optional<String>              description;
    private @Nullable Optional<Image>               image;
    private @Nullable Optional<Requirements>        requirements;
    private @Nullable Optional<UserInput>           userInput;
    private @Nullable Optional<Map<String, String>> env;
    private @Nullable Optional<Highlight>           highlight;
    private @Nullable Optional<Boolean>             discardable;
    private @Nullable Map<String, String>           additionalEnv;
    private @Nullable Optional<List<LogParser>>     logParsers;
    private @Nullable Optional<String>              decision;
    private @Nullable Optional<Map<String, String>> result;

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withTemplateBase(@Nullable StageDefinition template) {
        this.template = template;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withRecentBase(@Nullable StageDefinition base) {
        this.base = base;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withName(@Nullable String name) {
        this.name = Optional.ofNullable(name);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withDescription(@Nullable String desc) {
        this.description = Optional.ofNullable(desc);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withImage(@Nullable Image image) {
        this.image = Optional.ofNullable(image);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withRequirements(@Nullable Requirements requirements) {
        this.requirements = Optional.ofNullable(requirements);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withUserInput(@Nullable UserInput userInput) {
        this.userInput = Optional.ofNullable(userInput);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withEnvironment(@Nullable Map<String, String> env) {
        this.env = Optional.ofNullable(env);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withAdditionalEnvironment(@Nullable Map<String, String> env) {
        this.additionalEnv = env;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withLogParsers(@Nullable List<LogParser> logParsers) {
        this.logParsers = Optional.ofNullable(logParsers);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withHighlight(@Nullable Highlight highlight) {
        this.highlight = Optional.ofNullable(highlight);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageDefinitionBuilder withDiscardable(@Nullable Boolean discardable) {
        this.discardable = Optional.ofNullable(discardable);
        return this;
    }

    @Nonnull
    public StageDefinition build() {
        var base = Optional.ofNullable(this.base).or(() -> Optional.ofNullable(this.template));
        var name = either(this.name, base.map(StageDefinition::getName));
        Objects.requireNonNull(name);

        var env = either(this.env, base.map(StageDefinition::getEnvironment));

        if (this.additionalEnv != null) {
            if (env != null) {
                env = new HashMap<>(env);
                env.putAll(this.additionalEnv);
            } else {
                env = new HashMap<>(this.additionalEnv);
            }
        }

        return new StageDefinition(
                name,
                either(this.description, base.flatMap(StageDefinition::getDescription)),
                either(this.image, base.flatMap(StageDefinition::getImage)),
                either(this.requirements, base.flatMap(StageDefinition::getRequirements)),
                either(this.userInput, base.flatMap(StageDefinition::getRequires)),
                env,
                either(this.highlight, base.flatMap(StageDefinition::getHighlight)),
                either(this.discardable, base.map(StageDefinition::isDiscardable)),
                Optional.ofNullable(either(
                        Optional.ofNullable(this.template).map(StageDefinition::isPrivileged),
                        Optional.ofNullable(this.base).map(StageDefinition::isPrivileged)
                )).orElse(Boolean.FALSE),
                either(
                        this.logParsers,
                        Optional.of(this.template.getLogParsers())
                ),
                Optional.ofNullable(either(
                        Optional.ofNullable(this.template).map(StageDefinition::getIgnoreFailuresWithinExecutionGroup),
                        Optional.ofNullable(this.base).map(StageDefinition::getIgnoreFailuresWithinExecutionGroup)
                )).orElse(Boolean.FALSE),
                either(
                        Optional.ofNullable(this.template).map(StageDefinition::getTags),
                        Optional.ofNullable(this.base).map(StageDefinition::getTags)
                ),
                either(this.decision, base.map(StageDefinition::getDecision)),
                either(this.result, base.map(StageDefinition::getResult))
        );
    }

    @Nullable
    private <T> T either(@Nullable Optional<T> a, @Nonnull Optional<T> b) {
        if (a != null) {
            return a.orElse(null);
        } else {
            return b.orElse(null);
        }
    }
}
