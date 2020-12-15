package de.itdesigners.winslow.api.project;

import javax.annotation.Nullable;

public class LogLinesRequest {
    public @Nullable Long   skipLines;
    public @Nullable String expectingStageId;

    public LogLinesRequest(@Nullable Long skipLines, @Nullable String expectingStageId) {
        this.skipLines        = skipLines;
        this.expectingStageId = expectingStageId;
    }
}
