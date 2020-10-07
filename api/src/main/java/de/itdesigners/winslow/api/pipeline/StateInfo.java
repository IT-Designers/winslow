package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StateInfo stateInfo = (StateInfo) o;
        return hasEnqueuedStages == stateInfo.hasEnqueuedStages &&
                state == stateInfo.state &&
                Objects.equals(pauseReason, stateInfo.pauseReason) &&
                Objects.equals(description, stateInfo.description) &&
                Objects.equals(stageProgress, stateInfo.stageProgress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, pauseReason, description, stageProgress, hasEnqueuedStages);
    }
}
