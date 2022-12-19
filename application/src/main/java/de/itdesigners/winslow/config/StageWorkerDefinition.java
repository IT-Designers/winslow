package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public record StageWorkerDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull Optional<String> description,
        @Nonnull Image image,
        @Nonnull Requirements requirements,
        @Nonnull UserInput userInput,
        @Nonnull Map<String, String> environment,
        @Nonnull Optional<Highlight> highlight,
        boolean discardable,
        boolean privileged,
        @Nonnull List<LogParser> logParsers,
        boolean ignoreFailuresWithinExecutionGroup,
        @Nonnull List<UUID> nextStages) implements StageDefinition {

    public StageWorkerDefinition(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nullable Optional<String> description,
            @Nonnull Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput userInput,
            @Nullable Map<String, String> environment,
            @Nullable Optional<Highlight> highlight,
            boolean discardable,
            boolean privileged,
            @Nullable List<LogParser> logParsers,
            boolean ignoreFailuresWithinExecutionGroup,
            @Nullable List<UUID> nextStages) {
        this.id                                 = id;
        this.name                               = name;
        this.description                        = description != null ? description : Optional.empty();
        this.image                              = image;
        this.requirements                       = requirements != null ? requirements : new Requirements();
        this.userInput                          = userInput != null ? userInput : new UserInput();
        this.environment                        = environment != null ? environment : Collections.emptyMap();
        this.highlight                          = highlight != null ? highlight : Optional.empty();
        this.discardable                        = discardable;
        this.privileged                         = privileged;
        this.logParsers                         = logParsers != null ? logParsers : Collections.emptyList();
        this.ignoreFailuresWithinExecutionGroup = ignoreFailuresWithinExecutionGroup;
        this.nextStages                         = nextStages != null ? nextStages : Collections.emptyList();
        this.check();
    }

    public StageWorkerDefinition(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nonnull Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput userInput,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight,
            boolean discardable,
            boolean privileged,
            @Nullable List<LogParser> logParsers,
            boolean ignoreFailuresWithinExecutionGroup,
            @Nullable List<UUID> nextStages) {
        this(
                id,
                name.trim(),
                Optional.ofNullable(description).filter(s -> !s.isBlank()),
                image,
                requirements != null ? requirements : new Requirements(),
                userInput != null ? userInput : new UserInput(),
                environment != null ? environment : Collections.emptyMap(),
                Optional.ofNullable(highlight),
                discardable,
                privileged,
                logParsers != null ? logParsers : Collections.emptyList(),
                ignoreFailuresWithinExecutionGroup,
                nextStages != null ? nextStages : Collections.emptyList()
        );
    }

    @Override
    public void check() throws RuntimeException {
        StageDefinition.super.check();
        Objects.requireNonNull(this.image(), "The image of a stage must be set");

    }

    public static UUID idFromName(String name) {
        return new UUID(name.hashCode(), name.length());
    }

    @Override
    @Nonnull
    public Map<String, String> environment() {
        return environment;
    }
}
