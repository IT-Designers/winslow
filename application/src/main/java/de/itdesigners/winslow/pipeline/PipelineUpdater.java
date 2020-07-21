package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.LockHeart;
import de.itdesigners.winslow.LockedContainer;
import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.fs.LockException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PipelineUpdater {

    private static final Logger LOG = Logger.getLogger(PipelineUpdater.class.getSimpleName());

    private final @Nonnull Orchestrator                    orchestrator;
    private final @Nonnull String                          projectId;
    private final @Nonnull BaseRepository.Handle<Pipeline> pipelineHandle;

    private @Nullable Pipeline pipelineReadOnly;

    private final @Nonnull List<NoAccessUpdater>        independentUpdates = new ArrayList<>();
    private final @Nonnull List<ExclusiveAccessUpdater> pipelineUpdates    = new ArrayList<>();

    public PipelineUpdater(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nonnull BaseRepository.Handle<Pipeline> pipelineHandle) {
        this(orchestrator, projectId, null, pipelineHandle);
    }

    public PipelineUpdater(
            @Nonnull Orchestrator orchestrator,
            @Nonnull Pipeline pipelineReadOnly,
            @Nonnull BaseRepository.Handle<Pipeline> pipelineHandle) {
        this(orchestrator, pipelineReadOnly.getProjectId(), pipelineReadOnly, pipelineHandle);
    }

    private PipelineUpdater(
            @Nonnull Orchestrator orchestrator,
            @Nonnull String projectId,
            @Nullable Pipeline pipelineReadOnly,
            @Nonnull BaseRepository.Handle<Pipeline> pipelineHandle) {
        if (pipelineReadOnly != null && !pipelineReadOnly.getProjectId().equals(projectId)) {
            throw new RuntimeException("ProjectId does not match with the given Pipeline");
        }

        this.orchestrator     = orchestrator;
        this.projectId        = projectId;
        this.pipelineReadOnly = pipelineReadOnly;
        this.pipelineHandle   = pipelineHandle;
        this.searchForUpdates();
    }

    public void evaluate() throws IOException, LockException {
        searchForUpdates();
        evaluateUpdatesWithoutExclusivePipelineAccess();
        evaluateUpdatesWithExclusivePipelineAccess();
    }

    public void searchForUpdates() {
        ActiveExecutionGroupUpdate
                .check(orchestrator, projectId, pipelineReadOnly)
                .ifPresent(independentUpdates::add);

        StageElectionUpdate
                .check(orchestrator, projectId, pipelineReadOnly)
                .ifPresent(independentUpdates::add);

        AutoEnqueueUpdate
                .check(orchestrator, projectId, pipelineReadOnly)
                .ifPresent(independentUpdates::add);

        StageTimeoutDetection
                .check(orchestrator, projectId, pipelineReadOnly)
                .ifPresent(independentUpdates::add);
    }

    @Nonnull
    private String pId() {
        return projectId;
    }

    public void evaluateUpdatesWithoutExclusivePipelineAccess() {
        try {
            this.independentUpdates
                    .stream()
                    .peek(updater -> LOG.log(Level.INFO, pId() + " RO: " + updater.getClass().getSimpleName()))
                    .flatMap(updater -> updater.update(orchestrator).stream())
                    .forEach(this.pipelineUpdates::add);
        } finally {
            this.independentUpdates.clear();
        }
    }

    public boolean hasPipelineUpdatesThatRequireExclusiveAccess() {
        return !this.pipelineUpdates.isEmpty();
    }

    public void evaluateUpdatesWithExclusivePipelineAccess() throws IOException, LockException {
        if (hasPipelineUpdatesThatRequireExclusiveAccess()) {
            try (var container = pipelineHandle
                    .exclusive()
                    .orElseThrow(() -> new LockException("Failed to acquire exclusive pipeline access"));
                 var heart = new LockHeart(container.getLock())) {


                var updatedPipeline = performPipelineUpdates(container);

                for (int i = 0; i < 10 && updatedPipeline.isPresent(); ++i) {
                    this.pipelineReadOnly = updatedPipeline.get();
                    this.searchForUpdates();
                    this.evaluateUpdatesWithoutExclusivePipelineAccess();
                    updatedPipeline = performPipelineUpdates(container);
                }

            } finally {
                this.pipelineUpdates.clear();
            }
        }
    }

    private Optional<Pipeline> performPipelineUpdates(@Nonnull LockedContainer<Pipeline> container) throws LockException, IOException {
        try {
            var mostRecentPipelineUpdate = Optional.<Pipeline>empty();
            for (var update : this.pipelineUpdates) {
                LOG.log(Level.INFO, pId() + " RW: " + update.getClass().getSimpleName());
                var pipeline = update.update(orchestrator, container.get().orElse(null));
                if (pipeline != null) {
                    container.update(pipeline);
                    mostRecentPipelineUpdate = Optional.of(pipeline);
                }

            }
            return mostRecentPipelineUpdate;
        } finally {
            this.pipelineUpdates.clear();
        }
    }


    public interface NoAccessUpdater {
        @Nonnull
        Optional<ExclusiveAccessUpdater> update(@Nonnull Orchestrator orchestrator);
    }


    public interface ExclusiveAccessUpdater {
        /**
         * Returns a valid instance if an update is required
         *
         * @param orchestrator Current active {@link Orchestrator}
         * @param pipeline     {@link Pipeline} to try to update or null if it does not exist (yet)
         * @return Either null for no update performed or an updated {@link Pipeline} instance
         */
        @Nullable
        Pipeline update(@Nonnull Orchestrator orchestrator, @Nullable Pipeline pipeline);
    }
}
