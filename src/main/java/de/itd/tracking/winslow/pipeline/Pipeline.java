package de.itd.tracking.winslow.pipeline;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Pipeline {

    @Nonnull private final String      projectId;
    @Nonnull private final List<Stage> stages;

    private           boolean             pauseRequested     = false;
    @Nullable private PauseReason         pauseReason        = null;
    @Nullable private ResumeNotification  resumeNotification = null;
    @Nullable private List<EnqueuedStage> enqueuedStages     = new ArrayList<>();

    @Nonnull private  Strategy strategy;
    @Nullable private Stage    stage;

    public Pipeline(@Nonnull String projectId) {
        this.projectId = projectId;
        this.stages    = new ArrayList<>();
        this.strategy  = Strategy.MoveForwardUntilEnd;
    }

    public Pipeline(
            @Nonnull String projectId,
            boolean pauseRequested,
            @Nullable PauseReason pauseReason,
            @Nullable ResumeNotification resumeNotification,
            @Nullable List<EnqueuedStage> enqueuedStages,
            @Nullable List<Stage> completedStages,
            @Nonnull Strategy strategy,
            @Nullable Stage runningStage) {
        this.projectId          = projectId;
        this.pauseRequested     = pauseRequested;
        this.pauseReason        = pauseReason;
        this.resumeNotification = resumeNotification;
        this.enqueuedStages     = enqueuedStages;
        this.stages             = completedStages != null ? completedStages : new ArrayList<>();
        this.strategy           = strategy;
        this.stage              = runningStage;
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public void pushStage(@Nullable Stage stage) {
        if (this.stage != null) {
            this.stages.add(this.stage);
        }
        this.stage = stage;
    }

    public boolean updateStage(@Nonnull Stage stage) {
        if (this.stage != null && this.stage.getId().equals(stage.getId())) {
            this.stage = stage;
            return true;
        }
        return false;
    }

    @Nonnull
    public Optional<Stage> getRunningStage() {
        return Optional.ofNullable(this.stage);
    }

    public boolean finishRunningStage(@Nonnull Stage.State finishState) {
        return getRunningStage().map(stage -> {
            stage.finishNow(finishState);
            this.pushStage(null);
            return stage;
        }).isPresent();
    }

    @Nonnull
    @Transient
    public Optional<Stage> getMostRecentStage() {
        return getRunningStage().or(() -> {
            if (stages.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(stages.get(stages.size() - 1));
            }
        });
    }

    @Transient
    public int getStageCount() {
        return this.stages.size() + (getRunningStage().isPresent() ? 1 : 0);
    }


    @Nonnull
    @Transient
    public Stream<Stage> getAllStages() {
        return Stream.concat(getCompletedStages(), getRunningStage().stream());
    }

    @Nonnull
    public Optional<Stage> getStage(@Nonnull String id) {
        return getAllStages().filter(s -> s.getId().equals(id)).findFirst();
    }

    @Nonnull
    public Stream<Stage> getCompletedStages() {
        return stages.stream();
    }

    public void requestPause() {
        this.requestPause(null);
    }

    public void requestPause(@Nullable PauseReason reason) {
        this.pauseReason    = reason;
        this.pauseRequested = true;
    }

    public void clearPauseReason() {
        this.pauseReason = null;
    }

    public boolean isPauseRequested() {
        return this.pauseRequested;
    }

    @Nonnull
    public Optional<PauseReason> getPauseReason() {
        return Optional.ofNullable(this.pauseReason);
    }

    public void resume() {
        this.resume(null);
    }

    public void resume(@Nullable ResumeNotification notification) {
        this.pauseRequested     = false;
        this.resumeNotification = notification;
    }

    @Nonnull
    public Optional<ResumeNotification> getResumeNotification() {
        return Optional.ofNullable(resumeNotification);
    }

    public void resetResumeNotification() {
        this.resumeNotification = null;
    }

    @Nonnull
    public Optional<EnqueuedStage> peekNextStage() {
        if (this.enqueuedStages != null && !this.enqueuedStages.isEmpty()) {
            return Optional.ofNullable(this.enqueuedStages.get(0));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public Optional<EnqueuedStage> popNextStage() {
        if (this.enqueuedStages != null && !this.enqueuedStages.isEmpty()) {
            return Optional.ofNullable(this.enqueuedStages.remove(0));
        } else {
            return Optional.empty();
        }
    }

    public boolean hasEnqueuedStages() {
        return this.enqueuedStages != null && !this.enqueuedStages.isEmpty();
    }

    public void enqueueStage(@Nonnull StageDefinition definition) {
        this.enqueueStage(definition, Action.Execute);
    }

    public void enqueueStage(@Nonnull StageDefinition definition, @Nonnull Action action) {
        if (this.enqueuedStages == null) {
            this.enqueuedStages = new ArrayList<>();
        }
        this.enqueuedStages.add(new EnqueuedStage(definition, action));
    }

    @Nonnull
    public Optional<EnqueuedStage> removeEnqueuedStage(int index) {
        if (this.enqueuedStages != null && this.enqueuedStages.size() > index) {
            return Optional.of(this.enqueuedStages.remove(index));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public Stream<EnqueuedStage> getEnqueuedStages() {
        if (this.enqueuedStages == null) {
            return Stream.empty();
        } else {
            return this.enqueuedStages.stream();
        }
    }

    @Nonnull
    public Strategy getStrategy() {
        return this.strategy;
    }

    public void setStrategy(@Nonnull Strategy strategy) {
        this.strategy = strategy;
    }

    public enum Strategy {
        MoveForwardUntilEnd, MoveForwardOnce,
    }

    public enum PauseReason {
        ConfirmationRequired, FurtherInputRequired, StageFailure
    }

    public enum ResumeNotification {
        Confirmation
    }
}
