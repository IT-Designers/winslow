package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageInfo;
import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.util.TreeMap;

public class StageInfoConverter {

    @Nonnull
    public static StageInfo from(@Nonnull Stage stage) {
        return new StageInfo(
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
