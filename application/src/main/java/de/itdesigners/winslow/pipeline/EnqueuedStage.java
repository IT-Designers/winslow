package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnqueuedStage {

    private final @Nonnull StageDefinition definition;
    private final @Nonnull Action          action;
    private final          boolean         continueOnPreviousWorkspace;

    public EnqueuedStage(
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nullable Boolean continueOnPreviousWorkspace) {
        this.definition                  = definition;
        this.action                      = action;
        this.continueOnPreviousWorkspace = continueOnPreviousWorkspace != null && continueOnPreviousWorkspace;
    }

    @Nonnull
    public StageDefinition getDefinition() {
        return definition;
    }

    @Nonnull
    public Action getAction() {
        return action;
    }

    public boolean isContinuingOnPreviousWorkspace() {
        return this.continueOnPreviousWorkspace;
    }

}
