package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.UserInput;

import javax.annotation.Nonnull;
import java.util.Collections;

public class StageDefinitionInfoConverter {

    @Nonnull
    public static StageDefinitionInfo from(@Nonnull StageDefinition definition) {
        return new StageDefinitionInfo(
                definition.getName(),
                definition.getImage().map(ImageInfoConverter::from).orElse(null),
                definition.getRequires().map(UserInput::getEnvironment).orElseGet(Collections::emptyList),
                definition.getRequirements().map(ResourceInfoConverter::from).orElse(null)
        );
    }
}
