package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.project.HistoryEntry;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.TreeMap;

public class HistoryEntryConverter {

    @Nonnull
    public static HistoryEntry from(@Nonnull Stage stage) {
        return new HistoryEntry(
                stage.getId(),
                stage.getStartTime(),
                stage.getFinishTime().orElse(null),
                stage.getState(),
                stage.getAction(),
                stage.getDefinition().getName(),
                stage.getWorkspace().orElse(null),
                stage.getDefinition().getImage().map(ImageInfoConverter::from).orElse(null),
                new TreeMap<>(stage.getEnv()),
                new TreeMap<>(stage.getEnvPipeline()),
                new TreeMap<>(stage.getEnvSystem()),
                new TreeMap<>(stage.getEnvInternal())
        );
    }

    @Nonnull
    public static HistoryEntry from(@Nonnull EnqueuedStage stage) {
        return new HistoryEntry(
                null,
                null,
                null,
                null,
                stage.getAction(),
                stage.getDefinition().getName(),
                null,
                stage.getDefinition().getImage().map(ImageInfoConverter::from).orElse(null),
                new TreeMap<>(stage.getDefinition().getEnvironment()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }
}
