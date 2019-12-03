package de.itdesigners.winslow.api.project;

import javax.annotation.Nullable;

public class StateInfo {

    public final @Nullable State   state;
    public final @Nullable String  pauseReason;
    public final @Nullable String  mostRecentStage;
    public final @Nullable Integer stageProgress;
    public final           boolean hasEnqueuedStages;


    public StateInfo(
            @Nullable State state,
            @Nullable String pauseReason,
            @Nullable String mostRecentStage,
            @Nullable Integer stageProgress,
            boolean hasEnqueuedStages) {
        this.state             = state;
        this.pauseReason       = pauseReason;
        this.mostRecentStage   = mostRecentStage;
        this.stageProgress     = stageProgress;
        this.hasEnqueuedStages = hasEnqueuedStages;
    }
}
