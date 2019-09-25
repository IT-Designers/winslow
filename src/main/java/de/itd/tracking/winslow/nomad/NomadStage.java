package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.Stage;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;

public class NomadStage implements Stage {

    @Nonnull private final String          jobId;
    @Nonnull private final String          taskName;
    @Nonnull private final StageDefinition definition;
    @Nonnull private final Date            startTime;
    @Nonnull private final String          workspace;

    @Nullable private Date  finishTime;
    @Nullable private State finishState;

    public NomadStage(@Nonnull String jobId, @Nonnull String taskName, @Nonnull StageDefinition definition, @Nonnull String workspace) {
        this.jobId      = jobId;
        this.taskName   = taskName;
        this.definition = definition;
        this.workspace  = workspace;

        this.startTime   = new Date();
        this.finishTime  = null;
        this.finishState = null;
    }

    @Nonnull
    @Override
    public String getId() {
        return getTaskName();
    }

    @Nonnull
    String getJobId() {
        return this.jobId;
    }

    @Nonnull
    String getTaskName() {
        return this.taskName;
    }

    void finishNow(@Nonnull State finishState) {
        this.finishTime  = new Date();
        this.finishState = finishState;
    }

    @Nonnull
    @Override
    public StageDefinition getDefinition() {
        return this.definition;
    }

    @Nonnull
    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Nullable
    @Override
    public Date getFinishTime() {
        return this.finishTime;
    }

    @Nonnull
    @Override
    public State getState() {
        return Optional.ofNullable(finishState).orElse(State.Running);
    }

    @Override
    @Nonnull
    public String getWorkspace() {
        return workspace;
    }
}
