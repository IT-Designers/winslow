package de.itd.tracking.winslow;

import javax.annotation.Nonnull;

public interface PreparedStage {
    /**
     * Tries to actually start this prepared {@link PreparedStage}. Once started successfully,
     * the return value of this method remains the same: The started {@link Stage}
     *
     * @return The new {@link Stage} instance
     * @throws OrchestratorException If the start failed or already started
     */
    @Nonnull
    Stage start() throws OrchestratorException;
}
