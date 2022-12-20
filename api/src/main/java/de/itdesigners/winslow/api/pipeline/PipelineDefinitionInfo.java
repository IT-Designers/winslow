package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public record PipelineDefinitionInfo(
        @Nonnull String id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull UserInputInfo userInput,
        @Nonnull List<StageDefinitionInfo> stages,
        @Nonnull DeletionPolicy deletionPolicy,
        @Nonnull List<String> markers) {

    public PipelineDefinitionInfo(
            @Nonnull String id,
            @Nonnull String name,
            @Nonnull String description,
            @Nonnull UserInputInfo userInput,
            @Nonnull List<StageDefinitionInfo> stages,
            @Nonnull DeletionPolicy deletionPolicy,
            @Nonnull List<String> markers) {
        this.id             = id;
        this.name           = name;
        this.description    = description;
        this.userInput      = userInput;
        this.stages         = stages;
        this.deletionPolicy = deletionPolicy;
        this.markers        = markers;
    }
}
