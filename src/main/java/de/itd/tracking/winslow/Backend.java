package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public interface Backend {

    @Nonnull
    Stream<String> listStages() throws IOException;

    @Nonnull
    Optional<Stage.State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    @Nonnull
    Stream<LogEntry> getLogs(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    @Nonnull
    PreparedStageBuilder newStageBuilder(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull StageDefinition definition);
}
