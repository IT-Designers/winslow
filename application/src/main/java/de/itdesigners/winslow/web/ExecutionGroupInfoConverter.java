package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.ExecutionGroupInfo;
import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ExecutionGroupInfoConverter {

    @Nonnull
    public static ExecutionGroupInfo convert(@Nonnull ExecutionGroup group, boolean active) {
        return new ExecutionGroupInfo(
                group.getFullyQualifiedId(),
                group.isConfigureOnly(),
                StageDefinitionInfoConverter.from(group.getStageDefinition()),
                new TreeMap<>(group.getRangedValues().orElse(Collections.emptyMap())),
                group.getWorkspaceConfiguration(),
                group
                        .getStages()
                        .map(StageInfoConverter::from)
                        .collect(Collectors.toList()),
                active,
                group.getComment().orElse(null)
        );
    }
}
