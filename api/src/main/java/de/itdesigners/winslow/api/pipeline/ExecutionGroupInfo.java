package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class ExecutionGroupInfo {

    public final @Nonnull String                         id;
    public final          boolean                        configureOnly;
    public final @Nonnull StageDefinitionInfo            stageDefinition;
    public final @Nonnull Map<String, RangeWithStepSize> rangedValues;
    public final @Nonnull WorkspaceConfiguration         workspaceConfiguration;
    public final @Nonnull List<StageInfo>                stages;

    public ExecutionGroupInfo(
            @Nonnull String id,
            boolean configureOnly,
            @Nonnull StageDefinitionInfo stageDefinition,
            @Nonnull Map<String, RangeWithStepSize> rangedValues,
            @Nonnull WorkspaceConfiguration workspaceConfiguration,
            @Nonnull List<StageInfo> stages) {
        this.id                     = id;
        this.configureOnly          = configureOnly;
        this.stageDefinition        = stageDefinition;
        this.rangedValues           = rangedValues;
        this.workspaceConfiguration = workspaceConfiguration;
        this.stages                 = stages;
    }
}
