package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.PipelineInfo;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.UserInput;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.stream.Collectors;

public class PipelineInfoConverter {

    @Nonnull
    public static PipelineInfo from(@Nonnull String id, @Nonnull PipelineDefinition pipeline) {
        return new PipelineInfo(
                id,
                pipeline.getName(),
                pipeline.getDescription().orElse(null),
                pipeline
                        .getRequires()
                        .map(UserInput::getEnvironment)
                        .orElseGet(Collections::emptyList),
                pipeline
                        .getStages()
                        .stream()
                        .map(StageInfoConverter::from)
                        .collect(Collectors.toUnmodifiableList())
        );
    }
}
