package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExecutionGroupInfo {

    public final @Nonnull  String                   id;
    public final           boolean                  configureOnly;
    public final @Nonnull  StageDefinitionInfo stageDefinition;
    public final @Nonnull  Map<String, RangedValue> rangedValues;
    public final @Nonnull  WorkspaceConfiguration   workspaceConfiguration;
    public final @Nonnull  List<StageInfo>          stages;
    public final           boolean                  active;
    public final           boolean                  enqueued;
    public final @Nullable String                   comment;

    public ExecutionGroupInfo(
            @Nonnull String id,
            boolean configureOnly,
            @Nonnull StageDefinitionInfo stageDefinition,
            @Nonnull Map<String, RangedValue> rangedValues,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull List<StageInfo> stages,
            boolean active,
            boolean enqueued,
            @Nullable String comment) {
        this.id                     = id;
        this.configureOnly          = configureOnly;
        this.stageDefinition        = stageDefinition;
        this.rangedValues           = rangedValues;
        this.workspaceConfiguration = workspaceConfiguration;
        this.stages                 = stages;
        this.active                 = active;
        this.enqueued               = enqueued;
        this.comment                = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExecutionGroupInfo that = (ExecutionGroupInfo) o;
        return configureOnly == that.configureOnly && active == that.active && enqueued == that.enqueued && id.equals(
                that.id) && stageDefinition.equals(that.stageDefinition) && rangedValues.equals(that.rangedValues) && workspaceConfiguration.equals(
                that.workspaceConfiguration) && stages.equals(that.stages) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                configureOnly,
                stageDefinition,
                rangedValues,
                workspaceConfiguration,
                stages,
                active,
                enqueued,
                comment
        );
    }
}
