package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.DeletionPolicy;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Pipeline implements Cloneable {

    @Nonnull private final String      projectId;
    @Nonnull private final List<Stage> stages;

    private           boolean             pauseRequested     = false;
    private @Nullable PauseReason         pauseReason        = null;
    private @Nullable ResumeNotification  resumeNotification = null;
    private @Nullable List<EnqueuedStage> enqueuedStages     = new ArrayList<>();

    private @Nullable DeletionPolicy deletionPolicy;
    private @Nonnull  Strategy       strategy;
    private @Nullable Stage          stage;

    private int stageCounter;

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
            @Nullable DeletionPolicy deletionPolicy,
            @Nonnull Strategy strategy,
            @Nullable Stage runningStage,
            @Nullable Integer stageCounter) {
        this.projectId          = projectId;
        this.pauseRequested     = pauseRequested;
        this.pauseReason        = pauseReason;
        this.resumeNotification = resumeNotification;
        this.enqueuedStages     = enqueuedStages;
        this.stages             = completedStages != null ? completedStages : new ArrayList<>();
        this.deletionPolicy     = deletionPolicy;
        this.strategy           = strategy;
        this.stage              = runningStage;
        this.stageCounter       = stageCounter != null
                                  ? stageCounter
                                  : Optional.ofNullable(completedStages).map(List::size).orElse(0)
                                          + (runningStage != null ? 1 : 0);
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public void pushStage(@Nullable Stage stage) {
        if (this.stage != null) {
            this.stages.add(this.stage);
            this.stageCounter += 1;
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

    public boolean removeStage(@Nonnull String stageId) {
        for (int i = this.stages.size() - 1; i >= 0; --i) {
            if (this.stages.get(i).getId().equals(stageId)) {
                this.stages.remove(i);
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public Optional<Stage> getRunningStage() {
        return Optional.ofNullable(this.stage);
    }

    public boolean finishRunningStage(@Nonnull State finishState) {
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

    @Nonnull
    @Transient
    public Optional<Stage> getMostRecentStage(@Nonnull Action action) {
        return getRunningStage()
                .filter(stage -> stage.getAction() == action)
                .or(() -> {
                    for (var i = stages.size() - 1; i >= 0; --i) {
                        if (stages.get(i).getAction() == action) {
                            return Optional.of(stages.get(i));
                        }
                    }
                    return Optional.empty();
                });
    }

    /**
     * @return The number of stages ever completed for this pipeline
     */
    public int getStageCounter() {
        // return this.stages.size() + (getRunningStage().isPresent() ? 1 : 0);
        return this.stageCounter;
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
        this.enqueue(new EnqueuedStage(definition, action, null));
    }

    public void enqueueStageContinuation(@Nonnull StageDefinition definition, @Nonnull Action action) {
        this.enqueue(new EnqueuedStage(definition, action, true));
    }

    private void enqueue(@Nonnull EnqueuedStage stage) {
        if (this.enqueuedStages == null) {
            this.enqueuedStages = new ArrayList<>();
        }
        this.enqueuedStages.add(stage);
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

    @Nonnull
    public Optional<DeletionPolicy> getDeletionPolicy() {
        return Optional.ofNullable(this.deletionPolicy);
    }

    public void setDeletionPolicy(@Nullable DeletionPolicy policy) {
        this.deletionPolicy = policy;
    }

    public void setStrategy(@Nonnull Strategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public Pipeline clone() {
        return new Pipeline(
                this.projectId,
                pauseRequested,
                pauseReason,
                resumeNotification,
                enqueuedStages != null ? new ArrayList<>(enqueuedStages) : null,
                stages.stream().map(Stage::clone).collect(Collectors.toList()),
                deletionPolicy,
                strategy,
                stage,
                stageCounter
        );
    }

    public enum Strategy {
        MoveForwardUntilEnd, MoveForwardOnce,
    }

    public enum PauseReason {
        ConfirmationRequired,
        FurtherInputRequired,
        StageFailure,
        NoFittingNodeFound,
    }

    public enum ResumeNotification {
        Confirmation
    }
}
