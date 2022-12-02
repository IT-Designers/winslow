package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;

public class StageDefinitionInfoConverter {

    @Nonnull
    public static StageDefinitionInfo from(@Nonnull StageDefinition definition) {
        return new StageDefinitionInfo(
                definition.id(),
                definition.name(),
                ImageInfoConverter.from(definition.image()),
                definition.userInput().getEnvironment(),
                ResourceInfoConverter.from(definition.requirements()),
                definition.environment()
        );
    }
}
