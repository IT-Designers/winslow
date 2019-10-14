package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class Pipeline {

    @Nonnull private final String      projectId;
    @Nonnull private final List<Stage> stages = new ArrayList<>();

    private           boolean               pauseRequested     = false;
    @Nullable private PauseReason           pauseReason        = null;
    @Nullable private ResumeNotification    resumeNotification = null;
    @Nullable private Map<String, String>   env                = new HashMap<>();
    @Nullable private List<StageDefinition> enqueuedStages     = new ArrayList<>();

    @Nonnull private  Strategy strategy;
    @Nullable private Stage    stage;

    public Pipeline(@Nonnull String projectId) {
        this.projectId = projectId;
        this.strategy  = Strategy.MoveForwardUntilEnd;
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

    @Nonnull
    public Optional<Stage> getRunningStage() {
        return Optional.ofNullable(this.stage);
    }

    @Nonnull
    public Optional<Stage> getMostRecentStage() {
        return getRunningStage().or(() -> {
            if (stages.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(stages.get(stages.size() - 1));
            }
        });
    }

    public int getStageCount() {
        return this.stages.size() + (getRunningStage().isPresent() ? 1 : 0);
    }


    @Nonnull
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
        ;
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
    public Optional<StageDefinition> peekNextStage() {
        if (this.enqueuedStages != null && !this.enqueuedStages.isEmpty()) {
            return Optional.ofNullable(this.enqueuedStages.get(0));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public Optional<StageDefinition> popNextStage() {
        if (this.enqueuedStages != null && !this.enqueuedStages.isEmpty()) {
            return Optional.ofNullable(this.enqueuedStages.remove(0));
        } else {
            return Optional.empty();
        }
    }

    public void enqueueStage(@Nonnull StageDefinition definition) {
        if (this.enqueuedStages == null) {
            this.enqueuedStages = new ArrayList<>();
        }
        this.enqueuedStages.add(definition);
    }

    @Nonnull
    public Strategy getStrategy() {
        return this.strategy;
    }

    public void setStrategy(@Nonnull Strategy strategy) {
        this.strategy = strategy;
    }

    @Nonnull
    public Map<String, String> getEnvironment() {
        if (this.env == null) {
            this.env = new HashMap<>();
        }
        return this.env;
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
