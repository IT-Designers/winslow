package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageInfo;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.UserInput;

import javax.annotation.Nonnull;
import java.util.Collections;

public class StageInfoConverter {

    @Nonnull
    public static StageInfo from(@Nonnull StageDefinition definition) {
        return new StageInfo(
                definition.getName(),
                definition.getImage().map(ImageInfoConverter::from).orElse(null),
                definition.getRequires().map(UserInput::getEnvironment).orElseGet(Collections::emptyList)
        );
    }
}
