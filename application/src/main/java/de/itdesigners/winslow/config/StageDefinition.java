package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;

public record StageDefinition(@Nonnull UUID id,
                                    @Nonnull String name,
                                    @Nonnull String description,
                                    @Nonnull Image image,
                                    @Nonnull Requirements requirements,
                                    @Nonnull UserInput userInput,
                                    @Nonnull Map<String, String> environment,
                                    @Nonnull Highlight highlight,
                                    Boolean discardable,
                                    Boolean privileged,
                                    @Nonnull List<LogParser> logParsers,
                                    Boolean ignoreFailuresWithinExecutionGroup,
                                    @Nonnull List<String> tags,
                                    @Nonnull Map<String, String> result,
                                    @Nonnull StageType type,
                                    @Nonnull List<UUID> nextStages) {


    public StageDefinition(
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
            @Nullable List<String> tags,
            @Nullable Map<String, String> result,
            @Nullable StageType type,
            @Nullable List<UUID> nextStages) {
        this.id                                 = id != null ? id : idFromName(name);
        this.name                               = name;
        this.description                        = description != null ? description : "";
        this.image                              = image != null ? image : new Image();
        this.requirements                       = requirements != null ? requirements : new Requirements(
                null,
                null,
                null
        );
        this.userInput                          = userInput != null ? userInput : new UserInput(null, null);
        this.environment                        = environment != null ? environment : Collections.emptyMap();
        this.highlight                          = highlight != null ? highlight : new Highlight();
        this.discardable                        = discardable != null && discardable;
        this.privileged                         = privileged != null && privileged;
        this.logParsers                         = Optional
                .ofNullable(logParsers)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
        this.ignoreFailuresWithinExecutionGroup = ignoreFailuresWithinExecutionGroup != null && ignoreFailuresWithinExecutionGroup;
        this.tags                               = tags != null
                                                  ? Collections.unmodifiableList(tags)
                                                  : Collections.emptyList();
        this.result                             = result != null ? result : Collections.emptyMap();
        this.type                               = type != null ? type : StageType.Execution;
        this.nextStages                         = nextStages != null ? nextStages : Collections.emptyList();
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
    }

    public static UUID idFromName(String name) {
        return new UUID(name.hashCode(), name.length());
    }

    @Override
    public String toString() {

        return getClass()
                .getSimpleName() + "@{name='" + this.name + "',desc='" + this.description + "',image=" + this.image + ",userInput=" + this.userInput + ",type='" + this.type + "'}#" + this
                .hashCode();
    }
}
