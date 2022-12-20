package de.itdesigners.winslow.api.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.Optional;

public record WorkspaceConfiguration(
        @Nonnull WorkspaceMode mode,
        @Nullable String value,
        boolean sharedWithinGroup,
        boolean nestedWithinGroup
) {
    public WorkspaceConfiguration() {
        this(WorkspaceMode.INCREMENTAL, null, false, false);
    }

    public WorkspaceConfiguration(@Nonnull WorkspaceMode mode) {
        this(mode, null, false, false);
    }

    @ConstructorProperties({"mode", "value", "sharedWithinGroup", "nestedWithinGroup"})
    public WorkspaceConfiguration {
    }

    @Nonnull
    @Transient
    public Optional<String> optValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Only returns true if {@link #sharedWithinGroup()} is false and {@link #nestedWithinGroup} is true.
     *
     * @return Whether the multiple workspaces should be nested within the workspace of the group.
     */
    @Transient
    public boolean nestedWithinGroupExclusive() {
        // TODO controversial: applying (business-)logic rules in getter?
        return nestedWithinGroup && !sharedWithinGroup();
    }

    @Nonnull
    @Transient
    @CheckReturnValue
    public WorkspaceConfiguration withSharedWithinGroupExclusively() {
        return new WorkspaceConfiguration(mode, value, true, false);
    }

    @Nonnull
    @Transient
    @CheckReturnValue
    public WorkspaceConfiguration withNestedWithinGroupExclusively() {
        return new WorkspaceConfiguration(mode, value, false, true);
    }

    public enum WorkspaceMode {
        STANDALONE,
        INCREMENTAL,
        CONTINUATION,
    }
}


