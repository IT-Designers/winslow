package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Optional;

public record LogLinesRequest(
        @Nullable Long skipLines,
        @Nullable String expectingStageId) {

    @Nonnull
    @Transient
    public Optional<Long> optSkipLines() {
        return Optional.ofNullable(skipLines);
    }

    @Nonnull
    @Transient
    public Optional<String> optExpectingStageId() {
        return Optional.ofNullable(expectingStageId);
    }
}
