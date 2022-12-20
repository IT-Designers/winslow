package de.itdesigners.winslow.web;

import de.itdesigners.winslow.LogEntry;
import de.itdesigners.winslow.api.pipeline.LogEntryInfo;

import javax.annotation.Nonnull;

public class LogEntryInfoConverter {

    @Nonnull
    public static LogEntryInfo from(@Nonnull LogEntry log, long line, @Nonnull String stageId) {
        return new LogEntryInfo(
                log.time(),
                log.source(),
                log.error(),
                log.message(),
                line,
                stageId
        );
    }
}
