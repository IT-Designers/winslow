package de.itdesigners.winslow;

import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

public interface Backend extends Closeable, AutoCloseable {

    @Nonnull
    StageHandle submit(@Nonnull Submission submission) throws IOException;

    boolean isCapableOfExecuting(@Nonnull StageDefinition stageDefinition);

    boolean isCapableOfExecuting(@Nonnull Submission submission);
}
