package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record ExecutionGroupInfo(
        @Nonnull String id,
        boolean configureOnly,
        @Nonnull StageDefinitionInfo stageDefinition,
        @Nonnull Map<String, RangedValue> rangedValues,
        @Nonnull WorkspaceConfiguration workspaceConfiguration,
        @Nonnull List<StageInfo> stages,
        boolean active,
        boolean enqueued,
        @Nullable String comment) {

}
