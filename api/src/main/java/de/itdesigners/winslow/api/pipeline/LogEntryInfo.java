package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record LogEntryInfo(
        long time,
        @Nonnull LogSource source,
        boolean error,
        @Nonnull String message,
        long line,
        @Nonnull String stageId
) {
}
