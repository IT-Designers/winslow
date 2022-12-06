package de.itdesigners.winslow;

import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

public interface Backend extends Closeable, AutoCloseable {

    void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException;

    void stop(@Nonnull String stage) throws IOException;

    void kill(@Nonnull String stage) throws IOException;

    @Nonnull
    SubmissionResult submit(@Nonnull Submission submission) throws IOException;

    boolean isCapableOfExecuting(@Nonnull StageDefinition stage);
}
