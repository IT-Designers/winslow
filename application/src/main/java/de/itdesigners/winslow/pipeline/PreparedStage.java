package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.StageHandle;

import javax.annotation.Nonnull;

public interface PreparedStage {

    /**
     * Tries to actually execute this prepared {@link PreparedStage}. Once started successfully,
     * the return value of this method remains the same for all further calls: The started {@link Stage}
     *
     * @return The new {@link Stage} instance
     * @throws OrchestratorException If the start failed or already started
     */
    @Nonnull
    Result execute() throws OrchestratorException;

    /**
     * Uses the {@link PreparedStage} to configure the pipeline as if it would have
     * been started, executed and finished successfully, but without it being executed.
     * Once this method call succeeded, the return value for all further calls remains
     * the same: The configured {@link Stage}
     *
     * @return The new {@link Stage} instance
     * @throws OrchestratorException If the configuration failed
     */
    @Nonnull
    Result configure() throws OrchestratorException;

    class Result {
        private final @Nonnull Stage       stage;
        private final @Nonnull StageHandle handle;

        public Result(@Nonnull Stage stage, @Nonnull StageHandle handle) {
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
}
