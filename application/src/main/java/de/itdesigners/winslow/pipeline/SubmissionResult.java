package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.StageHandle;

import javax.annotation.Nonnull;

public class SubmissionResult {
    private final @Nonnull Stage       stage;
    private final @Nonnull StageHandle handle;

    public SubmissionResult(@Nonnull Stage stage, @Nonnull StageHandle handle) {
        this.stage  = stage;
        this.handle = handle;
    }

    @Nonnull
    public Stage getStage() {
        return stage;
    }

    @Nonnull
    public StageHandle getHandle() {
        return handle;
    }
}
