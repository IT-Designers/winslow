package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.PipelineInfo;
import de.itdesigners.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

public class PipelineInfoConverter {

    @Nonnull
    public static PipelineInfo from(@Nonnull String id, @Nonnull PipelineDefinition pipeline) {
        return new PipelineInfo(
                id,
                pipeline.name(),
                pipeline.description(),
                pipeline
                        .userInput().getEnvironment(),
                pipeline
                        .stages()
                        .stream()
                        .map(StageDefinitionInfoConverter::from)
                        .collect(Collectors.toUnmodifiableList()),
                pipeline.markers()
        );
    }
}
