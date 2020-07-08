package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageInfo;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.UserInput;
import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.TreeMap;

public class StageInfoConverter {

    @Nonnull
    public static StageInfo from(@Nonnull StageDefinition definition) {
        return new StageInfo(
                definition.getName(),
                definition.getImage().map(ImageInfoConverter::from).orElse(null),
                definition.getRequires().map(UserInput::getEnvironment).orElseGet(Collections::emptyList),
                definition.getRequirements().map(ResourceInfoConverter::from).orElse(null)
        );
    }


    @Nonnull
    public static de.itdesigners.winslow.api.project.StageInfo from(@Nonnull Stage stage) {
        return new de.itdesigners.winslow.api.project.StageInfo(
                stage.getFullyQualifiedId(),
                stage.getStartTime(),
                stage.getFinishTime().orElse(null),
                stage.getState(),
                stage.getId__().getHumanReadableGroupHint().orElse("unknown"),
                stage.getWorkspace().orElse(null),
                new TreeMap<>(stage.getEnv()),
                new TreeMap<>(stage.getEnvPipeline()),
                new TreeMap<>(stage.getEnvSystem()),
                new TreeMap<>(stage.getEnvInternal())
        );
    }
}
