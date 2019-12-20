package de.itdesigners.winslow;

import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public interface Backend {

    @Nonnull
    Stream<String> listStages() throws IOException;

    @Nonnull
    Optional<State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    void kill(@Nonnull String stage) throws IOException;

    @Nonnull
    SubmissionResult submit(@Nonnull Submission submission) throws IOException;

    boolean isCapableOfExecuting(@Nonnull StageDefinition stage);
}
