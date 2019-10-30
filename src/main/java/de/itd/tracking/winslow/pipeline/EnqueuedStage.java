package de.itd.tracking.winslow.pipeline;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;

public class EnqueuedStage {

    @Nonnull private final StageDefinition definition;
    @Nonnull private final Action          action;

    public EnqueuedStage(@Nonnull StageDefinition definition, @Nonnull Action action) {
        this.definition = definition;
        this.action     = action;
    }

    @Nonnull
    public StageDefinition getDefinition() {
        return definition;
    }

    @Nonnull
    public Action getAction() {
        return action;
    }

}
