package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public record StageInfo(
        @Nonnull String id,
        @Nullable Date startTime,
        @Nullable Date finishTime,
        @Nullable State state,
        @Nullable String workspace,
        @Nonnull Map<String, String> env,
        @Nonnull Map<String, String> envPipeline,
        @Nonnull Map<String, String> envSystem,
        @Nonnull Map<String, String> envInternal,
        @Nonnull Map<String, String> result) {

    @Nonnull
    @Transient
    public Optional<Date> optStartTime() {
        return Optional.ofNullable(startTime);
    }

    @Nonnull
    @Transient
    public Optional<Date> optFinishTime() {
        return Optional.ofNullable(finishTime);
    }

    @Nonnull
    @Transient
    public Optional<State> optState() {
        return Optional.ofNullable(state);
    }

    @Nonnull
    @Transient
    public Optional<String> optWorkspace() {
        return Optional.ofNullable(workspace);
    }
}
