package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Stage implements Cloneable {

    @Nonnull private final StageId id;

    @Nullable private String              workspace;
    @Nullable private Date                startTime;
    @Nullable private Date                finishTime;
    @Nullable private State               finishState;
    @Nullable private Map<String, String> env;
    @Nullable private Map<String, String> envPipeline;
    @Nullable private Map<String, String> envSystem;
    @Nullable private Map<String, String> envInternal;

    public Stage(@Nonnull StageId id, @Nullable String workspace) {
        this.id        = id;
        this.workspace = workspace;

        this.startTime   = null;
        this.finishTime  = null;
        this.finishState = null;
    }

    @ConstructorProperties({"id", "startTime", "workspace", "finishTime", "finishState", "env", "envPipeline", "envSystem", "envInternal"})
    public Stage(
            @Nonnull StageId id,
            @Nullable Date startTime,
            @Nullable String workspace,
            @Nullable Date finishTime,
            @Nullable State finishState,
            @Nullable Map<String, String> env,
            @Nullable Map<String, String> envPipeline,
            @Nullable Map<String, String> envSystem,
            @Nullable Map<String, String> envInternal) {
        this.id          = id;
        this.startTime   = startTime;
        this.workspace   = workspace;
        this.finishTime  = finishTime;
        this.finishState = finishState;
        this.env         = env;
        this.envPipeline = envPipeline;
        this.envSystem   = envSystem;
        this.envInternal = envInternal;
    }

    @Nonnull
    public StageId getId() {
        return this.id;
    }

    @Nonnull
    @Transient
    public String getFullyQualifiedId() {
        return this.id.getFullyQualified();
    }

    @Nonnull
    @Transient
    public String getProjectRelativeId() {
        return this.id.getProjectRelative();
    }

    public void startNow() {
        this.startTime = new Date();
    }

    public void finishNow(@Nonnull State finishState) {
        this.finishTime  = new Date();
        this.finishState = finishState;
    }

    @Nonnull
    public Optional<Date> getStartTime() {
        return Optional.ofNullable(startTime);
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
        return getFinishState().orElseGet(() -> {
            if (startTime == null) {
                return State.Preparing;
            } else {
                return State.Running;
            }
        });
    }

    /**
     * @return The relative path to the workspaces root directory to locate the workspace of this stage
     */
    @Nonnull
    public Optional<String> getWorkspace() {
        return Optional.ofNullable(workspace);
    }

    public void setWorkspace(@Nullable String workspace) {
        this.workspace = workspace;
    }

    @Nonnull
    public Map<String, String> getEnv() {
        if (this.env == null) {
            this.env = new HashMap<>();
        }
        return this.env;
    }

    @Nonnull
    public Map<String, String> getEnvPipeline() {
        if (this.envPipeline == null) {
            this.envPipeline = new HashMap<>();
        }
        return this.envPipeline;
    }

    @Nonnull
    public Map<String, String> getEnvSystem() {
        if (this.envSystem == null) {
            this.envSystem = new HashMap<>();
        }
        return this.envSystem;
    }

    @Nonnull
    public Map<String, String> getEnvInternal() {
        if (this.envInternal == null) {
            this.envInternal = new HashMap<>();
        }
        return this.envInternal;
    }
}
