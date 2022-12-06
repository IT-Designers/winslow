package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StageWorkerDefinitionInfo(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nullable String description,
        @Nullable ImageInfo image,
        @Nullable RequirementsInfo requiredResources,
        @Nullable UserInputInfo userInput,
        @Nullable Map<String, String> environment,
        @Nullable HighlightInfo highlight,
        @Nullable Boolean discardable,
        @Nullable Boolean privileged,
        @Nullable List<LogParserInfo> logParsers,
        @Nonnull Boolean ignoreFailuresWithinExecutionGroup,
        @Nonnull List<UUID> nextStages) implements StageDefinitionInfo {
}
