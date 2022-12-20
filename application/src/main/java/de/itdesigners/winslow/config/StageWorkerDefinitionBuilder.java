package de.itdesigners.winslow.config;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StageWorkerDefinitionBuilder {

    private @Nullable StageWorkerDefinition         template;
    private @Nullable StageWorkerDefinition         base;
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
    public StageWorkerDefinitionBuilder withTemplateBase(@Nullable StageWorkerDefinition template) {
        this.template = template;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withRecentBase(@Nullable StageWorkerDefinition base) {
        this.base = base;
        return this;
    }


    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withDescription(@Nullable String desc) {
        this.description = Optional.ofNullable(desc);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withImage(@Nullable Image image) {
        this.image = Optional.ofNullable(image);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withRequirements(@Nullable Requirements requirements) {
        this.requirements = Optional.ofNullable(requirements);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withUserInput(@Nullable UserInput userInput) {
        this.userInput = Optional.ofNullable(userInput);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withEnvironment(@Nullable Map<String, String> env) {
        this.env = Optional.ofNullable(env);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withAdditionalEnvironment(@Nullable Map<String, String> env) {
        this.additionalEnv = env;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withLogParsers(@Nullable List<LogParser> logParsers) {
        this.logParsers = Optional.ofNullable(logParsers);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withHighlight(@Nullable Highlight highlight) {
        this.highlight = Optional.ofNullable(highlight);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public StageWorkerDefinitionBuilder withDiscardable(@Nullable Boolean discardable) {
        this.discardable = Optional.ofNullable(discardable);
        return this;
    }

    @Nonnull
    public StageWorkerDefinition build() {
        var base = Optional.ofNullable(this.base).or(() -> Optional.ofNullable(this.template)).orElseThrow();
        var env  = either(this.env, Optional.of(base.environment()));

        if (this.additionalEnv != null) {
            if (env != null) {
                env = new HashMap<>(env);
                env.putAll(this.additionalEnv);
            } else {
                env = new HashMap<>(this.additionalEnv);
            }
        }

        return new StageWorkerDefinition(
                base.id(),
                base.name(),
                either(this.description, Optional.of(base.description())),
                Optional.ofNullable(this.template).map(StageWorkerDefinition::nextStages).orElse(null),
                either(this.image, Optional.of(base.image())),
                either(this.requirements, Optional.of(base.requirements())),
                either(this.userInput, Optional.of(base.userInput())),
                env,
                either(this.highlight, Optional.of(base.highlight())),
                either(
                        this.logParsers,
                        Optional.of(this.template.logParsers())
                ),
                either(this.discardable, Optional.of(base.discardable())),
                Optional.ofNullable(either(
                        Optional.ofNullable(this.template).map(StageWorkerDefinition::privileged),
                        Optional.ofNullable(this.base).map(StageWorkerDefinition::privileged)
                )).orElse(Boolean.FALSE),
                Optional.ofNullable(either(
                        Optional
                                .ofNullable(this.template)
                                .map(StageWorkerDefinition::ignoreFailuresWithinExecutionGroup),
                        Optional.ofNullable(this.base).map(StageWorkerDefinition::ignoreFailuresWithinExecutionGroup)
                )).orElse(Boolean.FALSE)
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
