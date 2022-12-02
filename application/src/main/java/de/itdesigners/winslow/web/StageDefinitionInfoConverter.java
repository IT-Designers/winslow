package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;

public class StageDefinitionInfoConverter {

    @Nonnull
    public static StageDefinitionInfo from(@Nonnull StageDefinition definition) {
        return new StageDefinitionInfo(
                definition.getId(),
                definition.getName(),
                ImageInfoConverter.from(definition.getImage()),
                definition.getRequires().getEnvironment(),
                ResourceInfoConverter.from(definition.getRequirements()),
                definition.getEnvironment()
        );
    }
}
