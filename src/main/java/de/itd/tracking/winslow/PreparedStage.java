package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a {@link de.itd.tracking.winslow.config.Stage} of
 * which the execution is ready to be started.
 * The execution can only be started once, further start
 * attempts will not succeed.
 */
public interface PreparedStage {

    @Nonnull
    Path getWorkspaceDirectory();

    /**
     * This call does return a value only on first call, all
     * following calls will return a {@link Optional#empty()}
     *
     * @return The {@link RunningStage} instance for the started {@link de.itd.tracking.winslow.config.Stage}
     *         this instance represents.
     * @throws OrchestratorException If the {@link de.itd.tracking.winslow.config.Stage} could not be started
     */
    @Nonnull
    Optional<RunningStage> start() throws OrchestratorException;
}
