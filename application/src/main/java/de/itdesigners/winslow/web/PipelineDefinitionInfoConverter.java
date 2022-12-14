package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;

public class PipelineDefinitionInfoConverter {

    @Nonnull
    public static PipelineDefinitionInfo from(@Nonnull String id, @Nonnull PipelineDefinition pipeline) {
        return new PipelineDefinitionInfo(
                id,
                pipeline.name(),
                pipeline.description(),
                UserInputInfoConverter.from(pipeline.userInput()),
                pipeline
                        .stages()
                        .stream()
                        .map(StageDefinitionInfoConverter::from)
                        .toList(),
                pipeline.deletionPolicy(),
                pipeline.markers()
        );
    }
}
