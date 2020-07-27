package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;

public class StateInfo {

    public final @Nullable State   state;
    public final @Nullable String  pauseReason;
    public final @Nullable String  description;
    public final @Nullable Integer stageProgress;
    public final           boolean hasEnqueuedStages;


    public StateInfo(
            @Nullable State state,
            @Nullable String pauseReason,
            @Nullable String description,
            @Nullable Integer stageProgress,
            boolean hasEnqueuedStages) {
        this.state             = state;
        this.pauseReason       = pauseReason;
        this.description       = description;
        this.stageProgress     = stageProgress;
        this.hasEnqueuedStages = hasEnqueuedStages;
    }
}
