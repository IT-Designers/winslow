package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

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
                        .collect(Collectors.toUnmodifiableList()),
                pipeline.deletionPolicy(),
                pipeline.markers()
        );
    }
}
