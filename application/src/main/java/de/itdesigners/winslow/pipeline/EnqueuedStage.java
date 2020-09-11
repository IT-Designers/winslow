package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class EnqueuedStage {

    private final @Nonnull  StageDefinition        definition;
    private final @Nonnull  Action                 action;
    private final @Nonnull  WorkspaceConfiguration workspaceConfiguration;
    private final @Nullable String                 comment;

    @Deprecated
    public EnqueuedStage(
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nullable WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment) {
        this.definition             = definition;
        this.action                 = action;
        this.workspaceConfiguration = workspaceConfiguration != null
                                      ? workspaceConfiguration
                                      : new WorkspaceConfiguration();
        this.comment                = comment;
    }

    @Nonnull
    public StageDefinition getDefinition() {
        return definition;
    }

    @Nonnull
    public Action getAction() {
        return action;
    }

    @Nonnull
    public WorkspaceConfiguration getWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    @Nonnull
    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }
}

