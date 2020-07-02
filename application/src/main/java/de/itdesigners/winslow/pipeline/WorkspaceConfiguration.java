package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Optional;

public class WorkspaceConfiguration {
    private final @Nonnull WorkspaceMode mode;
    private final @Nullable String value;

    public WorkspaceConfiguration() {
        this(WorkspaceMode.INCREMENTAL, null);
    }

    @ConstructorProperties({"mode", "value"})
    public WorkspaceConfiguration(@Nonnull WorkspaceMode mode, @Nullable String value) {
        this.mode  = mode;
        this.value = value;
    }

    @Nonnull
    public WorkspaceMode getMode() {
        return mode;
    }

    @Nonnull
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    public enum WorkspaceMode {
        STANDALONE,
        INCREMENTAL,
        CONTINUATION,
    }
}


