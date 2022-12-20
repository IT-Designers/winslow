package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Optional;

public record StateInfo(
        @Nullable State state,
        @Nullable String pauseReason,
        @Nullable String description,
        @Nullable Integer stageProgress,
        boolean hasEnqueuedStages) {

    @Nonnull
    @Transient
    public Optional<State> optState() {
        return Optional.ofNullable(state);
    }

    @Nonnull
    @Transient
    public Optional<String> optPauseReason() {
        return Optional.ofNullable(pauseReason);
    }

    @Nonnull
    @Transient
    public Optional<String> optDescription() {
        return Optional.ofNullable(description);
    }

    @Nonnull
    @Transient
    public Optional<Integer> optStageProgress() {
        return Optional.ofNullable(stageProgress);
    }

}
