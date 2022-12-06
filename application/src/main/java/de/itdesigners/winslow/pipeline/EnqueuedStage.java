package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.StageWorkerDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class EnqueuedStage {

    private final @Nonnull StageWorkerDefinition definition;
    private final @Nonnull  Action                 action;
    private final @Nonnull  WorkspaceConfiguration workspaceConfiguration;
    private final @Nullable String                 comment;

    @Deprecated
    public EnqueuedStage(
            @Nonnull StageWorkerDefinition definition,
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
    public StageWorkerDefinition getDefinition() {
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

