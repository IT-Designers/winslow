package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.*;
import java.util.stream.Stream;

public class Pipeline implements Cloneable {

    private final @Nonnull String               projectId;
    private final @Nonnull List<ExecutionGroup> executionHistory;
    private final @Nonnull List<ExecutionGroup> executionQueue;
    private final @Nonnull List<ExecutionGroup> activeExecutions;

    private           boolean                              pauseRequested     = false;
    private @Nullable PauseReason                          pauseReason        = null;
    private @Nullable ResumeNotification                   resumeNotification = null;
    private @Nullable DeletionPolicy                       deletionPolicy;
    private @Nullable WorkspaceConfiguration.WorkspaceMode workspaceConfigurationMode;

    private int executionCounter;

    public Pipeline(@Nonnull String projectId) {
        this.projectId        = projectId;
        this.executionCounter = 0;
        this.activeExecutions = new ArrayList<>();
        this.executionHistory = new ArrayList<>();
        this.executionQueue   = new ArrayList<>();
    }

    @ConstructorProperties({
            "projectId",
            "executionHistory",
            "enqueuedExecutions",
            "activeExecutionGroups",
            "pauseRequested",
            "pauseReason",
            "resumeNotification",
            "deletionPolicy",
            "workspaceConfigurationMode",
            "executionCounter"
    })
    public Pipeline(
            @Nonnull String projectId,
            @Nullable List<ExecutionGroup> executionHistory,
            @Nullable List<ExecutionGroup> enqueuedExecutions,
            @Nullable List<ExecutionGroup> activeExecutionGroups,
            boolean pauseRequested,
            @Nullable PauseReason pauseReason,
            @Nullable ResumeNotification resumeNotification,
            @Nullable DeletionPolicy deletionPolicy,
            @Nullable WorkspaceConfiguration.WorkspaceMode workspaceConfigurationMode,
            int executionCounter) {
        this.projectId                  = projectId;
        this.executionHistory           = Optional.ofNullable(executionHistory).orElseGet(ArrayList::new);
        this.executionQueue             = Optional.ofNullable(enqueuedExecutions).orElseGet(ArrayList::new);
        this.activeExecutions           = Optional.ofNullable(activeExecutionGroups).orElseGet(ArrayList::new);
        this.pauseRequested             = pauseRequested;
        this.pauseReason                = pauseReason;
        this.resumeNotification         = resumeNotification;
        this.deletionPolicy             = deletionPolicy;
        this.workspaceConfigurationMode = workspaceConfigurationMode;
        this.executionCounter           = executionCounter;
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public void archiveActiveExecution(@Nonnull ExecutionGroup executionGroup) throws ExecutionGroupStillHasRunningStagesException, IllegalStateException {
        if (executionGroup.getRunningStages().findAny().isPresent()) {
            throw new ExecutionGroupStillHasRunningStagesException(this, executionGroup);
        } else if (this.activeExecutions.remove(executionGroup)) {
            this.executionHistory.add(executionGroup);
        } else {
            throw new IllegalStateException("Execution group not found in active executions");
        }
    }

    /**
     * @return Whether a new {@link ExecutionGroup} was marked as actively executing (false if there is none)
     */
    @Transient
    public boolean retrieveNextActiveExecution() {
        if (!this.executionQueue.isEmpty()) {
            this.activeExecutions.add(this.executionQueue.remove(0));
            return true;
        } else {
            return false;
        }
    }

    @Transient
    public boolean canRetrieveNextActiveExecution() {
        return this.activeExecutions.isEmpty() && !this.executionQueue.isEmpty();
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
    public Stream<ExecutionGroup> getExecutionHistory() {
        return this.executionHistory.stream();
    }

    @Nonnull
    public Stream<ExecutionGroup> getActiveExecutionGroups() {
        return this.activeExecutions.stream();
    }

    @Nonnull
    @Transient
    public Stream<ExecutionGroup> getActiveOrNextExecutionGroup() {
        if (!activeExecutions.isEmpty()) {
            return activeExecutions.stream();
        } else if (!this.executionQueue.isEmpty()) {
            return Stream.of(this.executionQueue.get(0));
        } else {
            return Stream.empty();
        }
    }

    @Nonnull
    @Transient
    public Stream<ExecutionGroup> getActiveOrPreviousExecutionGroup() {
        if (!activeExecutions.isEmpty()) {
            return activeExecutions.stream();
        } else {
            return getPreviousExecutionGroup().stream();
        }
    }

    @Nonnull
    @Transient
    public Optional<ExecutionGroup> getPreviousExecutionGroup() {
        if (!this.executionHistory.isEmpty()) {
            return Optional.of(this.executionHistory.get(this.executionHistory.size() - 1));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    @Transient
    public Stream<ExecutionGroup> getActiveAndPastExecutionGroups() {
        return Stream.concat(this.executionHistory.stream(), getActiveExecutionGroups());
    }

    @Nonnull
    public Stream<ExecutionGroup> getEnqueuedExecutions() {
        return this.executionQueue.stream();
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

    @Transient
    public boolean hasEnqueuedStages() {
        return this.activeExecutions.stream().anyMatch(ExecutionGroup::hasRemainingExecutions) || !this.executionQueue.isEmpty();
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
    public ExecutionGroupId enqueueConfiguration(@Nonnull StageDefinition definition, @Nullable String comment) {
        var id = incrementAndGetNextExecutionGroupId(definition.name());
        this.executionQueue.add(new ExecutionGroup(id, definition, comment));
        return id;
    }

    @Nonnull
    public ExecutionGroupId enqueueSingleExecution(
            @Nonnull StageDefinition definition,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment,
            @Nullable ExecutionGroupId parentId) {
        var id = incrementAndGetNextExecutionGroupId(definition.name());
        this.executionQueue.add(new ExecutionGroup(id, definition, workspaceConfiguration, comment, parentId));
        return id;
    }

    @Nonnull
    public ExecutionGroupId enqueueRangedExecution(
            @Nonnull StageDefinition definition,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull Map<String, RangedValue> rangedValues) {
        var id = incrementAndGetNextExecutionGroupId(definition.name());
        this.executionQueue.add(new ExecutionGroup(id, definition, rangedValues, workspaceConfiguration));
        return id;
    }

    @Nonnull
    public Optional<DeletionPolicy> getDeletionPolicy() {
        return Optional.ofNullable(this.deletionPolicy);
    }

    public void setDeletionPolicy(@Nullable DeletionPolicy policy) {
        this.deletionPolicy = policy;
    }

    @Nonnull
    public Optional<WorkspaceConfiguration.WorkspaceMode> getWorkspaceConfigurationMode() {
        return Optional.ofNullable(workspaceConfigurationMode);
    }

    public void setWorkspaceConfigurationMode(@Nonnull WorkspaceConfiguration.WorkspaceMode mode) {
        this.workspaceConfigurationMode = mode;
    }

    public enum PauseReason {
        ConfirmationRequired,
        FurtherInputRequired,
        StageFailure,
        NoFittingNodeFound,
    }

    public enum ResumeNotification {
        Confirmation,
        RunSingleThenPause,
    }
}
