package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;

public class PipelineDefinitionInfoConverter {

    @Nonnull
    public static PipelineDefinitionInfo from(@Nonnull PipelineDefinition pipeline) {
        return new PipelineDefinitionInfo(
                pipeline.id(),
                pipeline.name(),
                pipeline.optDescription().orElse(""),
                UserInputInfoConverter.from(pipeline.userInput()),
                pipeline
                        .stages()
                        .stream()
                        .map(StageDefinitionInfoConverter::from)
                        .toList(),
                pipeline.environment(),
                pipeline.deletionPolicy(),
                pipeline.groups(),
                pipeline.belongsToProject(),
                pipeline.publicAccess()
        );
    }

    @Nonnull
    public static PipelineDefinition reverse(@Nonnull PipelineDefinitionInfo pipeline) {
        return new PipelineDefinition(
                pipeline.id(),
                pipeline.name(),
                pipeline.description(),
                UserInputInfoConverter.reverse(pipeline.userInput()),
                pipeline.stages().stream().map(StageDefinitionInfoConverter::reverse).toList(),
                pipeline.environment(),
                pipeline.deletionPolicy(),
                pipeline.groups(),
                pipeline.belongsToProject(),
                pipeline.publicAccess()
        );
    }
}
