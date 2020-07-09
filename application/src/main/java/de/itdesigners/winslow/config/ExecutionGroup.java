package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.RangeWithStepSize;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.NamedId;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageId;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ExecutionGroup {

    private final @Nonnull  ExecutionGroupId                   id;
    private final           boolean                            configureOnly;
    private final @Nonnull  StageDefinition                    stageDefinition;
    /**
     * Some kind of ordered Map is important here so that the {@link #groupCounter} retrieves the entries
     * in a reliable order
     */
    private final @Nullable TreeMap<String, RangeWithStepSize> rangedValues;
    private final @Nonnull  WorkspaceConfiguration             workspaceConfiguration;
    private final @Nonnull  List<Stage>                        stages;

    private int groupCounter;

    /**
     * Configure-only constructor
     *
     * @param id              The id of this {@link ExecutionGroup}
     * @param stageDefinition {@link StageDefinition} being configured
     */
    public ExecutionGroup(@Nonnull ExecutionGroupId id, @Nonnull StageDefinition stageDefinition) {
        this(id, true, stageDefinition, new TreeMap<>(), new WorkspaceConfiguration(), new ArrayList<>(), 0);
    }

    /**
     * Single-execution constructor
     *
     * @param id                     The id of this {@link ExecutionGroup}
     * @param stageDefinition        {@link StageDefinition} to build the {@link Stage} for
     * @param workspaceConfiguration {@link WorkspaceConfiguration} to apply
     */
    public ExecutionGroup(
            @Nonnull ExecutionGroupId id,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull WorkspaceConfiguration workspaceConfiguration) {
        this(id, false, stageDefinition, Collections.emptyMap(), workspaceConfiguration, new ArrayList<>(), 0);
    }


    /**
     * Group-execution constructor
     *
     * @param id                     The id of this {@link ExecutionGroup}
     * @param stageDefinition        {@link StageDefinition} to build the {@link Stage}s for
     * @param rangedValues           Values to vary between executions
     * @param workspaceConfiguration {@link WorkspaceConfiguration} to apply
     */
    public ExecutionGroup(
            @Nonnull ExecutionGroupId id,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Map<String, RangeWithStepSize> rangedValues,
            @Nonnull WorkspaceConfiguration workspaceConfiguration) {
        this(id, false, stageDefinition, rangedValues, workspaceConfiguration, new ArrayList<>(), 0);
    }


    /**
     * Deserialization constructor
     */
    @ConstructorProperties({"id", "configureOnly", "stageDefinition", "rangedValues", "workspaceConfiguration", "stages", "groupCounter"})
    public ExecutionGroup(
            @Nonnull ExecutionGroupId id,
            boolean configureOnly,
            @Nonnull StageDefinition stageDefinition,
            @Nullable Map<String, RangeWithStepSize> rangedValues,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull List<Stage> stages,
            int groupCounter) {
        this.id                     = id;
        this.configureOnly          = configureOnly;
        this.stageDefinition        = stageDefinition;
        this.rangedValues           = new TreeMap<>(rangedValues);
        this.workspaceConfiguration = workspaceConfiguration;
        this.stages                 = stages;
        this.groupCounter           = groupCounter;
    }


    /**
     * Legacy {@link Stage}-Import constructor
     */
    @ConstructorProperties({"id, definition", "action", "startTime", "workspace", "finishTime", "finishState", "env", "envInternal", "workspaceConfiguration"})
    public ExecutionGroup(
            @Nonnull String id,
            @Nonnull StageDefinition definition,
            @Nonnull Action action,
            @Nonnull Date startTime,
            @Nullable String workspace,
            @Nullable Date finishTime,
            @Nullable State finishState,
            @Nullable Map<String, String> env,
            @Nullable Map<String, String> envPipeline,
            @Nullable Map<String, String> envSystem,
            @Nullable Map<String, String> envInternal,
            @Nullable WorkspaceConfiguration workspaceConfiguration) {
        this(NamedId.parseLegacyExecutionGroupId(id), definition, Optional.ofNullable(workspaceConfiguration)
                                                                          .orElseGet(() -> new WorkspaceConfiguration(
                                                                                  WorkspaceConfiguration.WorkspaceMode.INCREMENTAL,
                                                                                  null
                                                                          )));
        this.addStage(new Stage(
                this.id.generateStageId(null),
                startTime,
                workspace,
                finishTime,
                finishState,
                env,
                envPipeline,
                envSystem,
                envInternal
        ));
    }

    @Nonnull
    public ExecutionGroupId getId__() {
        return id;
    }

    @Nonnull
    @Transient
    public String getFullyQualifiedId() {
        return id.getFullyQualified();
    }

    @Nonnull
    @Transient
    public String getProjectRelativeId() {
        return id.getProjectRelative();
    }

    public void addStage(@Nonnull Stage stage) {
        this.stages.add(stage);
        this.incrementGroupCounter();
    }

    /**
     * @return A {@link StageDefinition} to create a stage for or {@link Optional#empty()} if no further stages need to be executed
     */
    @Nonnull
    @Transient
    public Optional<Pair<StageId, StageDefinition>> getNextStageDefinition() {
        if (this.configureOnly) {
            return Optional.empty();
        } else if (this.rangedValues != null && !this.rangedValues.isEmpty()) {
            var counter = getGroupCounter();
            var map     = new HashMap<>(this.stageDefinition.getEnvironment());

            for (var entry : this.rangedValues.entrySet()) {
                map.put(
                        entry.getKey(),
                        String.valueOf(entry.getValue().getValue(counter % entry.getValue().getStepCount()))
                );
                counter /= entry.getValue().getStepCount();
            }

            return Optional.of(new Pair<>(
                    this.id.generateStageId(getGroupCounter() + 1),
                    new StageDefinition(
                            stageDefinition.getName(),
                            stageDefinition.getDescription().orElse(null),
                            stageDefinition.getImage().orElse(null),
                            stageDefinition.getRequirements().orElse(null),
                            stageDefinition.getRequires().orElse(null),
                            map,
                            stageDefinition.getHighlight().orElse(null),
                            stageDefinition.isDiscardable(),
                            stageDefinition.isPrivileged()
                    )
            ));
        } else if (this.stages.isEmpty()) {
            return Optional.of(new Pair<>(this.id.generateStageId(null), this.stageDefinition));
        } else {
            return Optional.empty();
        }
    }


    /**
     * @return All {@link Stage}s associated with this {@link ExecutionGroup}
     */
    @Nonnull
    @Transient
    public Stream<Stage> getStages() {
        return this.stages.stream();
    }

    @Transient
    public int getStagesCount() {
        return this.stages.size();
    }

    /**
     * @return In comparision to {@link ExecutionGroup#getStages()}, this returns only {@link Stage}s that are currently {@link State#Running}
     */
    @Nonnull
    @Transient
    public Stream<Stage> getRunningStages() {
        return this.stages.stream().filter(s -> s.getFinishState().isEmpty());
    }

    /**
     * @return In comparision to {@link ExecutionGroup#getStages()}, this returns only {@link Stage}s that are hav ecompleted ({@link State#Failed} or {@link State#Succeeded})
     */
    @Nonnull
    @Transient
    public Stream<Stage> getCompletedStages() {
        return this.stages.stream().filter(s -> s.getFinishState().isPresent());
    }

    @Nonnull
    public WorkspaceConfiguration getWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    /**
     * @return The current group counter value
     */
    public int getGroupCounter() {
        return this.groupCounter;
    }

    /**
     * Increments the group counter, indicating to step on further in {@link ExecutionGroup#getNextStageDefinition()}
     */
    public void incrementGroupCounter() {
        this.groupCounter = getGroupCounter() + 1;
    }

    /**
     * @return The expected group size if all spawned stages succeed. This is more of a guess and does not represent a limit of allowed stages.
     */
    @Transient
    public int getExpectedGroupSize() {
        return Optional.ofNullable(this.rangedValues).map(m -> m
                .values()
                .stream()
                .map(RangeWithStepSize::getStepCount)
                .reduce(0, Integer::sum)
        ).orElseGet(() -> {
            if (configureOnly) {
                return 0;
            } else {
                return 1;
            }
        });
    }

    @Transient
    public boolean hasRemainingExecutions() {
        return getGroupCounter() < getExpectedGroupSize();
    }

    public boolean isConfigureOnly() {
        return configureOnly;
    }

    @Nonnull
    public StageDefinition getStageDefinition() {
        return stageDefinition;
    }

    /**
     * @return If non-empty: the varying values but unmodifiable
     */
    @Nonnull
    public Optional<Map<String, RangeWithStepSize>> getRangedValues() {
        return Optional.ofNullable(rangedValues).map(Collections::unmodifiableMap);
    }

    /**
     * @param stage The new {@link Stage}-data to update the with the id matching one
     * @return Whether a {@link Stage} for the given id was known and therefore the given update was stored
     * @throws StageIsArchivedAndNotAllowedToChangeException If the existing {@link Stage} for the given id has already finished
     */
    public boolean updateStage(@Nonnull Stage stage) throws StageIsArchivedAndNotAllowedToChangeException {
        return this.updateStage(stage.getFullyQualifiedId(), old -> Optional.of(stage));
    }

    public boolean updateStage(
            @Nonnull String stageId,
            @Nonnull Function<Stage, Optional<Stage>> updater) throws StageIsArchivedAndNotAllowedToChangeException {
        for (int n = this.stages.size() - 1; n >= 0; --n) {
            if (this.stages.get(n).getFullyQualifiedId().equals(stageId)) {
                if (this.stages.get(n).getFinishState().isEmpty()) {
                    final int index = n;
                    updater.apply(this.stages.get(index)).ifPresentOrElse(
                            updated -> this.stages.set(index, updated),
                            () -> this.stages.remove(index)
                    );
                    return true;
                } else {
                    throw new StageIsArchivedAndNotAllowedToChangeException(this, this.stages.get(n));
                }
            }
        }
        return false;
    }
}
