package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public record StageWorkerDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<UUID> nextStages,
        @Nonnull Image image,
        @Nonnull Requirements requirements,
        @Nonnull UserInput userInput,
        @Nonnull Map<String, String> environment,
        @Nonnull Highlight highlight,
        @Nonnull List<LogParser> logParsers,
        boolean discardable,
        boolean privileged,
        boolean ignoreFailuresWithinExecutionGroup) implements StageDefinition {

    public StageWorkerDefinition(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nonnull Image image) {
        this(id, name, null, null, image, null, null, null, null, null, false, false, false);
    }

    public StageWorkerDefinition(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nullable List<UUID> nextStages,
            @Nonnull Image image) {
        this(id, name, null, nextStages, image, null, null, null, null, null, false, false, false);
    }

    public StageWorkerDefinition(
            @Nullable UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable List<UUID> nextStages,
            @Nonnull Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput userInput,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight,
            @Nullable List<LogParser> logParsers,
            boolean discardable,
            boolean privileged,
            boolean ignoreFailuresWithinExecutionGroup) {
        this.id                                 = id != null ? id : StageDefinition.idFromName(name);
        this.name                               = name;
        this.description                        = description != null ? description : "";
        this.nextStages                         = nextStages != null ? nextStages : Collections.emptyList();
        this.image                              = image;
        this.requirements                       = requirements != null ? requirements : new Requirements();
        this.userInput                          = userInput != null ? userInput : new UserInput();
        this.environment                        = environment != null ? environment : Collections.emptyMap();
        this.highlight                          = highlight != null ? highlight : new Highlight();
        this.logParsers                         = logParsers != null ? logParsers : Collections.emptyList();
        this.discardable                        = discardable;
        this.privileged                         = privileged;
        this.ignoreFailuresWithinExecutionGroup = ignoreFailuresWithinExecutionGroup;
        this.check();
    }

    @Override
    public void check() throws RuntimeException {
        StageDefinition.super.check();
        Objects.requireNonNull(this.image(), "The image of a stage must be set");
    }
}
