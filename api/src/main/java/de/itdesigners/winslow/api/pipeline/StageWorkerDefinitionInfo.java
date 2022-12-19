package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StageWorkerDefinitionInfo(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull ImageInfo image,
        @Nonnull RequirementsInfo requiredResources,
        @Nonnull UserInputInfo userInput,
        @Nonnull Map<String, String> environment,
        @Nonnull HighlightInfo highlight,
        boolean discardable,
        boolean privileged,
        @Nonnull List<LogParserInfo> logParsers,
        boolean ignoreFailuresWithinExecutionGroup,
        @Nonnull List<UUID> nextStages) implements StageDefinitionInfo {
}
