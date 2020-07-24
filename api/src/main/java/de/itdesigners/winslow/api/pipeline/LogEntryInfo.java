package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LogEntryInfo extends LogEntry {

    public final           long   line;
    public final @Nullable String stageId;

    public LogEntryInfo(long line, @Nullable String stageId, @Nonnull LogEntry entry) {
        super(entry.getTime(), entry.getSource(), entry.isError(), entry.getMessage());
        this.line    = line;
        this.stageId = stageId;
    }
}
