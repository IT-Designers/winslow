package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public record PipelineInfo(

        @Nonnull String id,
        @Nonnull String name,
        @Nullable String desc,
        @Nonnull List<String> requiredEnvVariables,
        @Nonnull List<StageDefinitionInfo> stages,
        @Nonnull List<String> markers) {

    public PipelineInfo(
            @Nonnull String id,
            @Nonnull String name,
            @Nullable String desc,
            @Nonnull List<String> requiredEnvVariables,
            @Nonnull List<StageDefinitionInfo> stages,
            @Nonnull List<String> markers) {
        this.id                   = id;
        this.name                 = name;
        this.desc                 = desc;
        this.requiredEnvVariables = requiredEnvVariables;
        this.stages               = stages;
        this.markers              = markers;
    }
}
