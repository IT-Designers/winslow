package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.api.project.DeletionPolicy;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.RangeWithStepSize;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;
import java.util.stream.Stream;

public class Pipeline implements Cloneable {

    private final @Nonnull String               projectId;
    private final @Nonnull List<ExecutionGroup> executionHistory;
    private final @Nonnull List<ExecutionGroup> executionQueue;
    private @Nullable      ExecutionGroup       activeExecution;

    private           boolean                              pauseRequested     = false;
    private @Nullable PauseReason                          pauseReason        = null;
    private @Nullable ResumeNotification                   resumeNotification = null;
    private @Nullable DeletionPolicy                       deletionPolicy;
    private @Nonnull  Strategy                             strategy;
    private @Nullable WorkspaceConfiguration.WorkspaceMode workspaceConfigurationMode;

    private int executionCounter;

    public Pipeline(@Nonnull String projectId) {
        this.projectId        = projectId;
        this.strategy         = Strategy.MoveForwardUntilEnd;
        this.executionCounter = 0;

        this.executionHistory = new ArrayList<>();
        this.executionQueue   = new ArrayList<>();
    }

    /**
     * Legacy import constructor
     */
    @ConstructorProperties({"projectId", "pauseRequested", "pauseReason", "resumeNotification", "enqueuedStages", "completedStages", "deletionPolicy", "strategy", "runningStage", "stageCounter", "workspaceConfigurationMode"})
    public Pipeline(
            @Nonnull String projectId,
            boolean pauseRequested,
            @Nullable PauseReason pauseReason,
            @Nullable ResumeNotification resumeNotification,
            @Nullable List<EnqueuedStage> enqueuedStages,
            // Conversion through import constructor {@link ExecutionGroup#ExecutionGroup(String, StageDefinition, Action, Date, String, Date, State, Map, Map, Map, Map, WorkspaceConfiguration)}
            @Nullable List<ExecutionGroup> completedStages,
            @Nullable DeletionPolicy deletionPolicy,
            @Nonnull Strategy strategy,
            // Conversion through import constructor {@link ExecutionGroup#ExecutionGroup(String, StageDefinition, Action, Date, String, Date, State, Map, Map, Map, Map, WorkspaceConfiguration)}
            @Nullable ExecutionGroup runningStage,
            @Nullable Integer stageCounter,
            @Nullable WorkspaceConfiguration.WorkspaceMode workspaceConfigurationMode) {
        this.projectId          = projectId;
        this.pauseRequested     = pauseRequested;
        this.pauseReason        = pauseReason;
        this.resumeNotification = resumeNotification;
        this.executionQueue     = new ArrayList<>();
        this.executionHistory   = completedStages != null ? completedStages : new ArrayList<>();
        this.deletionPolicy     = deletionPolicy;
        this.strategy           = strategy;
        this.activeExecution    = runningStage;
        this.executionCounter   = stageCounter != null
                                  ? stageCounter
                                  : Optional.ofNullable(completedStages).map(List::size).orElse(0)
                                          + (runningStage != null ? 1 : 0);

        this.workspaceConfigurationMode = workspaceConfigurationMode;

        if (enqueuedStages != null) {
            enqueuedStages.stream().map(es -> {
                switch (es.getAction()) {
                    case Execute:
                        return new ExecutionGroup(
                                incrementAndGetNextExecutionGroupId(es.getDefinition().getName()),
                                es.getDefinition(),
                                es.getWorkspaceConfiguration()
                        );
                    case Configure:
                        return new ExecutionGroup(
                                incrementAndGetNextExecutionGroupId(es.getDefinition().getName()),
                                es.getDefinition()
                        );
                    default:
                        throw new RuntimeException("Unexpected action for legacy storage " + es.getAction());
                }
            }).forEach(this.executionQueue::add);
        }
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public void archiveActiveExecution() throws NullPointerException, ExecutionGroupStillHasRunningStagesException {
        Objects.requireNonNull(this.activeExecution);

        if (this.activeExecution.getRunningStages().count() > 0) {
            throw new ExecutionGroupStillHasRunningStagesException(this, this.activeExecution);
        } else {
            this.executionHistory.add(this.activeExecution);
            this.activeExecution = null;
        }
    }

    /**
     * @return Whether a new {@link ExecutionGroup} was marked as actively executing (false if there is none)
     * @throws ThereIsStillAnActiveExecutionGroupException If there is still an {@link ExecutionGroup} being executed
     */
    public boolean retrieveNextActiveExecution() throws ThereIsStillAnActiveExecutionGroupException {
        if (this.activeExecution != null) {
            throw new ThereIsStillAnActiveExecutionGroupException(this, this.activeExecution);
        }

        if (!this.executionQueue.isEmpty()) {
            this.activeExecution = this.executionQueue.remove(0);
            return true;
        } else {
            return false;
        }
    }

    public boolean activeExecutionGroupCouldSpawnFurtherStages() {
        return this.activeExecution != null && this.activeExecution.hasRemainingExecutions();
    }

    public boolean activeExecutionGroupHasNoFailedStages() {
        return getActiveExecutionGroup()
                .filter(group -> !group.isConfigureOnly())
                .stream()
                .flatMap(ExecutionGroup::getStages)
                .map(Stage::getState)
                .noneMatch(state -> state == State.Failed);
    }

    public boolean canRetrieveNextActiveExecution() {
        return this.activeExecution == null && !this.executionQueue.isEmpty();
    }

    /**
     * Ignores {@link ExecutionGroup} if it is active
     *
     * @param id The id of the {@link ExecutionGroup} to remove
     * @return The removed {@link ExecutionGroup} or {@link Optional#empty()} if not found
     */
    public Optional<ExecutionGroup> removeExecutionGroup(@Nonnull String id) {

        for (int i = 0; i < this.executionQueue.size(); ++i) {
            if (this.executionQueue.get(i).getFullyQualifiedId().equals(id)) {
                return Optional.of(this.executionQueue.remove(i));
            }
        }

        for (int i = 0; i < this.executionHistory.size(); ++i) {
            if (this.executionHistory.get(i).getFullyQualifiedId().equals(id)) {
                return Optional.of(this.executionHistory.remove(i));
            }
        }

        return Optional.empty();
    }

    @Nonnull
    public Optional<ExecutionGroup> getActiveExecutionGroup() {
        return Optional.ofNullable(this.activeExecution);
    }

    @Nonnull
    public Stream<ExecutionGroup> getPresentAndPastExecutionGroups() {
        return Stream.concat(this.executionHistory.stream(), getActiveExecutionGroup().stream());
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


    public boolean hasEnqueuedStages() {
        return (this.activeExecution != null && this.activeExecution.hasRemainingExecutions()) || !this.executionQueue.isEmpty();
    }

    public int getExecutionCounter() {
        return executionCounter;
    }

    @Nonnull
    private ExecutionGroupId incrementAndGetNextExecutionGroupId(@Nonnull String stageName) {
        this.executionCounter += 1;
        return new ExecutionGroupId(getProjectId(), executionCounter, stageName);
    }

    @Nonnull
    public ExecutionGroupId enqueueConfiguration(@Nonnull StageDefinition definition) {
        var id = incrementAndGetNextExecutionGroupId(definition.getName());
        this.executionQueue.add(new ExecutionGroup(id, definition));
        return id;
    }

    @Nonnull
    public ExecutionGroupId enqueueSingleExecution(
            @Nonnull StageDefinition definition,
            @Nonnull WorkspaceConfiguration workspaceConfiguration) {
        var id = incrementAndGetNextExecutionGroupId(definition.getName());
        this.executionQueue.add(new ExecutionGroup(id, definition, workspaceConfiguration));
        return id;
    }

    @Nonnull
    public ExecutionGroupId enqueueRangedExecution(
            @Nonnull StageDefinition definition,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull Map<String, RangeWithStepSize> rangedValues) {
        var id = incrementAndGetNextExecutionGroupId(definition.getName());
        this.executionQueue.add(new ExecutionGroup(id, definition, rangedValues, workspaceConfiguration));
        return id;
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

    @Nonnull
    public Optional<WorkspaceConfiguration.WorkspaceMode> getWorkspaceConfigurationMode() {
        return Optional.ofNullable(workspaceConfigurationMode);
    }

    public void setWorkspaceConfigurationMode(@Nonnull WorkspaceConfiguration.WorkspaceMode mode) {
        this.workspaceConfigurationMode = mode;
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
