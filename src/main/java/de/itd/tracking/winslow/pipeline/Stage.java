package de.itd.tracking.winslow.pipeline;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Stage {

    @Nonnull private final String          id;
    @Nonnull private final StageDefinition definition;
    @Nonnull private final Action          action;
    @Nonnull private final Date            startTime;
    @Nonnull private final String          workspace;

    @Nullable private Date                finishTime;
    @Nullable private State               finishState;
    @Nullable private Map<String, String> env;
    @Nullable private Map<String, String> envInternal;

    public Stage(
            @Nonnull String id,
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nonnull String workspace) {
        this.id         = id;
        this.definition = definition;
        this.action     = action;
        this.workspace  = workspace;

        this.startTime   = new Date();
        this.finishTime  = null;
        this.finishState = null;
    }

    @ConstructorProperties({"id, definition", "action", "startTime", "workspace", "finishTime", "finishState", "env", "envInternal"})
    public Stage(
            @Nonnull String id,
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nonnull Date startTime,
            @Nonnull String workspace,
            @Nullable Date finishTime,
            @Nullable State finishState,
            @Nullable Map<String, String> env,
            @Nullable Map<String, String> envInternal) {
        this.id          = id;
        this.definition  = definition;
        this.action      = action;
        this.startTime   = startTime;
        this.workspace   = workspace;
        this.finishTime  = finishTime;
        this.finishState = finishState;
        this.env         = env;
        this.envInternal = envInternal;
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
    public Action getAction() {
        return this.action;
    }

    @Nonnull
    public Date getStartTime() {
        return startTime;
    }

    @Nonnull
    public Optional<Date> getFinishTime() {
        return Optional.ofNullable(this.finishTime);
    }

    @Nonnull
    public Optional<State> getFinishState() {
        return Optional.ofNullable(this.finishState);
    }

    @Nonnull
    @Transient
    public State getState() {
        return getFinishState().orElse(State.Running);
    }

    @Nonnull
    public String getWorkspace() {
        return workspace;
    }

    @Nonnull
    public Map<String, String> getEnv() {
        if (this.env == null) {
            this.env = new HashMap<>();
        }
        return this.env;
    }

    @Nonnull
    public Map<String, String> getEnvInternal() {
        if (this.envInternal == null) {
            this.envInternal = new HashMap<>();
        }
        return this.envInternal;
    }

    public enum State {
        Running, Paused, Succeeded, Failed
    }
}
