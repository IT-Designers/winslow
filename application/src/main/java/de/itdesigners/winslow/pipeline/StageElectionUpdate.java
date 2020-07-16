package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.itdesigners.winslow.pipeline.CommonUpdateConstraints.*;

public class StageElectionUpdate implements PipelineUpdater.NoAccessUpdater {

    private static final Logger LOG = Logger.getLogger(StageElectionUpdate.class.getSimpleName());

    private final @Nonnull String projectId;

    public StageElectionUpdate(@Nonnull String projectId) {
        this.projectId = projectId;
    }

    @Nonnull
    public static Optional<PipelineUpdater.NoAccessUpdater> search(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly) {
        try {
            ensureIsNotLocked(orchestrator, projectId);
            ensureNoElectionIsRunning(orchestrator, projectId);
            ensureHasStageDefinitionToDeploy(pipelineReadOnly);

            return Optional.of(new StageElectionUpdate(projectId));

        } catch (PreconditionNotMetException e) {
            LOG.log(Level.FINE, "Missing precondition for stage update: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Nonnull
    @Override
    public Optional<PipelineUpdater.ExclusiveAccessUpdater> update(@Nonnull Orchestrator orchestrator) {
        new Thread(() -> {
            try {
                var duration = 2_000L;
                var puffer   = 100L;
                if (orchestrator.getElectionManager().maybeStartElection(projectId, duration + puffer)) {
                    LockBus.ensureSleepMs(duration);
                    orchestrator.getElectionManager().closeElection(projectId);
                }
            } catch (LockException | IOException e) {
                LOG.log(Level.SEVERE, "Failed to start election for project " + projectId);
            }
        }).start();
        return Optional.empty();
    }
}
