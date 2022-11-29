package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Optional;

public class WorkspaceConfiguration {

    private final @Nonnull  WorkspaceMode mode;
    private final @Nullable String        value;
    private final           boolean sharedWithinGroup;
    private final           boolean nestedWithinGroup;

    public WorkspaceConfiguration() {
        this(WorkspaceMode.INCREMENTAL, null, null, null);
    }

    @ConstructorProperties({"mode", "value", "sharedWithinGroup", "nestedWithinGroup"})
    public WorkspaceConfiguration(
            @Nonnull WorkspaceMode mode,
            @Nullable String value,
            @Nullable Boolean sharedWithinGroup,
            @Nullable Boolean nestedWithinGroup) {
        this.mode              = mode;
        this.value             = value;
        this.sharedWithinGroup = sharedWithinGroup != null && sharedWithinGroup;
        this.nestedWithinGroup = nestedWithinGroup != null && nestedWithinGroup;
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

    /**
     * This option has no effect, if {@link #isSharedWithinGroup()} is set to true and will in fact never return
     * true in that case.
     * @return Whether the multiple workspaces should be nested within the workspace of the group.
     */
    public boolean isNestedWithinGroup() {
        // TODO controversial: applying (business-)logic rules in getter?
        return nestedWithinGroup && !isSharedWithinGroup();
    }

    public enum WorkspaceMode {
        STANDALONE,
        INCREMENTAL,
        CONTINUATION,
    }
}


