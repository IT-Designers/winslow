package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Optional;

public class WorkspaceConfiguration {

    private final @Nonnull  WorkspaceMode mode;
    private final @Nullable String        value;
    private final           boolean       sharedWithinGroup;

    public WorkspaceConfiguration() {
        this(WorkspaceMode.INCREMENTAL, null, null);
    }

    @ConstructorProperties({"mode", "value", "sharedWithinGroup"})
    public WorkspaceConfiguration(
            @Nonnull WorkspaceMode mode,
            @Nullable String value,
            @Nullable Boolean sharedWithinGroup) {
        this.mode              = mode;
        this.value             = value;
        this.sharedWithinGroup = sharedWithinGroup != null && sharedWithinGroup;
    }

    @Nonnull
    public WorkspaceMode getMode() {
        return mode;
    }

    @Nonnull
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    public boolean isSharedWithinGroup() {
        return sharedWithinGroup;
    }

    public enum WorkspaceMode {
        STANDALONE,
        INCREMENTAL,
        CONTINUATION,
    }
}


