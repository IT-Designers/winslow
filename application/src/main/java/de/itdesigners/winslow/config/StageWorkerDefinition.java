package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public record StageWorkerDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull Image image,
        @Nonnull Requirements requirements,
        @Nonnull UserInput userInput,
        @Nonnull Map<String, String> environment,
        @Nonnull Highlight highlight,
        @Nonnull Boolean discardable,
        @Nonnull Boolean privileged,
        @Nonnull List<LogParser> logParsers,
        @Nonnull Boolean ignoreFailuresWithinExecutionGroup,
        @Nonnull List<UUID> nextStages) implements StageDefinition {


    public StageWorkerDefinition(
            // null-able for backwards compatibility
            @Nullable UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput userInput,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight,
            @Nullable Boolean discardable,
            @Nullable Boolean privileged,
            @Nullable List<LogParser> logParsers,
            @Nullable Boolean ignoreFailuresWithinExecutionGroup,
            @Nullable List<UUID> nextStages) {
        this.id                                 = id != null ? id : idFromName(name);
        this.name                               = name;
        this.description                        = description != null ? description : "";
        this.image                              = image != null ? image : new Image();
        this.requirements                       = requirements != null ? requirements : Requirements.createDefault();
        this.userInput                          = userInput != null ? userInput : new UserInput(null, null);
        this.environment                        = environment != null ? environment : Collections.emptyMap();
        this.highlight                          = highlight != null ? highlight : new Highlight(null);
        this.discardable                        = discardable != null && discardable;
        this.privileged                         = privileged != null && privileged;
        this.logParsers                         = Optional
                .ofNullable(logParsers)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
        this.ignoreFailuresWithinExecutionGroup = ignoreFailuresWithinExecutionGroup != null && ignoreFailuresWithinExecutionGroup;

        this.nextStages                         = nextStages != null ? nextStages : Collections.emptyList();
        this.check();
    }


    public static UUID idFromName(String name) {
        return new UUID(name.hashCode(), name.length());
    }

    @Override
    public String toString() {

        return getClass()
                .getSimpleName() + "@{name='" + this.name + "',description='" + this.description + "',image=" + this.image + ",userInput=" + this.userInput + ",type='" + this.getClass().getSimpleName() + "'}#" + this
                .hashCode();
    }
}
