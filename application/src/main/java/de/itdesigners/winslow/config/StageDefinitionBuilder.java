package de.itdesigners.winslow.config;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StageDefinitionBuilder {

    private @Nullable StageDefinition               template;
    private @Nullable StageDefinition               base;
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
        var base = Optional.ofNullable(this.base).or(() -> Optional.ofNullable(this.template)).orElseThrow();
        var env = either(this.env, Optional.of(base.environment()));

        if (this.additionalEnv != null) {
            if (env != null) {
                env = new HashMap<>(env);
                env.putAll(this.additionalEnv);
            } else {
                env = new HashMap<>(this.additionalEnv);
            }
        }

        return new StageDefinition(
                base.id(),
                base.name(),
                either(this.description, Optional.of(base.description())),
                either(this.image, Optional.of(base.image())),
                either(this.requirements, Optional.of(base.requirements())),
                either(this.userInput, Optional.of(base.userInput())),
                env,
                either(this.highlight, Optional.of(base.highlight())),
                either(this.discardable, Optional.of(base.discardable())),
                Optional.ofNullable(either(
                        Optional.ofNullable(this.template).map(StageDefinition::privileged),
                        Optional.ofNullable(this.base).map(StageDefinition::privileged)
                )).orElse(Boolean.FALSE),
                either(
                        this.logParsers,
                        Optional.of(this.template.logParsers())
                ),
                Optional.ofNullable(either(
                        Optional.ofNullable(this.template).map(StageDefinition::ignoreFailuresWithinExecutionGroup),
                        Optional.ofNullable(this.base).map(StageDefinition::ignoreFailuresWithinExecutionGroup)
                )).orElse(Boolean.FALSE),
                either(
                        Optional.ofNullable(this.template).map(StageDefinition::tags),
                        Optional.ofNullable(this.base).map(StageDefinition::tags)
                ),
                either(this.result, Optional.of(base.result())),
                template.type(),
                Optional.ofNullable(this.template).map(StageDefinition::nextStages).orElse(null)
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
