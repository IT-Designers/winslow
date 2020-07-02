package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnqueuedStage {

    private final @Nonnull StageDefinition        definition;
    private final @Nonnull Action                 action;
    private final @Nonnull WorkspaceConfiguration workspaceConfiguration;

    public EnqueuedStage(
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nullable WorkspaceConfiguration workspaceConfiguration) {
        this.definition             = definition;
        this.action                 = action;
        this.workspaceConfiguration = workspaceConfiguration != null
                                      ? workspaceConfiguration
                                      : new WorkspaceConfiguration();
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
}

