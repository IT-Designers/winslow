package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;

public class Stage {

    @Nonnull private final String          id;
    @Nonnull private final StageDefinition definition;
    @Nonnull private final Date            startTime;
    @Nonnull private final String          workspace;

    @Nullable private Date  finishTime;
    @Nullable private State finishState;

    public Stage(
            @Nonnull String id,
            @Nonnull StageDefinition definition,
            @Nonnull String workspace) {
        this.id         = id;
        this.definition = definition;
        this.workspace  = workspace;

        this.startTime   = new Date();
        this.finishTime  = null;
        this.finishState = null;
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    public void finishNow(@Nonnull State finishState) {
        this.finishTime  = new Date();
        this.finishState = finishState;
    }

    @Nonnull
    public StageDefinition getDefinition() {
        return this.definition;
    }

    @Nonnull
    public Date getStartTime() {
        return startTime;
    }

    @Nullable
    public Date getFinishTime() {
        return this.finishTime;
    }

    @Nonnull
    public State getState() {
        return Optional.ofNullable(finishState).orElse(State.Running);
    }

    @Nonnull
    public String getWorkspace() {
        return workspace;
    }

    public enum State {
        Running, Paused, Succeeded, Failed
    }
}
