package de.itdesigners.winslow;

import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockBusElectionManagerAdapter {

    private static final Logger LOG = Logger.getLogger(LockBusElectionManagerAdapter.class.getSimpleName());

    private final @Nonnull String          nodeName;
    private final @Nonnull ElectionManager electionManager;
    private final @Nonnull Orchestrator    orchestrator;

    public LockBusElectionManagerAdapter(
            @Nonnull String nodeName,
            @Nonnull ElectionManager electionManager,
            @Nonnull Orchestrator orchestrator) {
        this.nodeName        = nodeName;
        this.electionManager = electionManager;
        this.orchestrator    = orchestrator;
    }

    public static void setupAdapters(
            @Nonnull String nodeName,
            @Nonnull ElectionManager electionManager,
            @Nonnull Orchestrator orchestrator,
            @Nonnull LockBus lockBus) {
        new LockBusElectionManagerAdapter(nodeName, electionManager, orchestrator).setupAdapters(lockBus);
    }

    public void setupAdapters(@Nonnull LockBus lockBus) {
        this.electionManager.registerOnElectionStarted(this::handleElectionStarted);
        this.electionManager.registerOnElectionClosed(this::handleElectionClosed);

        lockBus.registerEventListener(Event.Command.ELECTION_START, this::handleElectionStartEvent);
        lockBus.registerEventListener(Event.Command.ELECTION_STOP, this::handleElectionStopEvent);
        lockBus.registerEventListener(
                Event.Command.ELECTION_PARTICIPATION,
                this::handleElectionParticipationEvent
        );
    }

    private void handleElectionStartEvent(@Nonnull Event event) {
        try {
            this.electionManager.onElectionStarted(
                    event.getSubject(),
                    event.getIssuer(),
                    event.getTime(),
                    event.getDuration()
            );
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to process election started event: " + event, e);
        }
    }

    private void handleElectionStopEvent(@Nonnull Event event) {
        try {
            this.electionManager.onElectionClosed(event.getSubject());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to process election close event: " + event, e);
        }
    }

    private void handleElectionParticipationEvent(@Nonnull Event event) {
        try {
            this.electionManager.onNewElectionParticipant(event.getSubject(), event.getIssuer());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to process election participation event: " + event, e);
        }
    }


    private void handleElectionStarted(@Nonnull Election election) {
        orchestrator
                .getPipelineUnsafe(election.getProjectId())
                .filter(orchestrator::isStageStateUpdateAvailable)
                .filter(p -> orchestrator.hasResourcesToSpawnStage(p).orElse(Boolean.FALSE))
                .filter(p -> orchestrator.isCapableOfExecutingNextStage(p).orElse(Boolean.FALSE))
                .flatMap(Pipeline::getActiveOrNextExecutionGroup)
                .ifPresent(activeGroup -> {
                    var requiredResources = orchestrator.getRequiredResources(activeGroup.getStageDefinition());
                    var participation     = orchestrator.judgeParticipationScore(requiredResources);

                    try {
                        electionManager.participate(election, participation);
                    } catch (IOException | LockException e) {
                        LOG.log(
                                Level.SEVERE,
                                "Failed to participate in election for paroject " + election.getProjectId(),
                                e
                        );
                    }
                });
    }

    private void handleElectionClosed(@Nonnull Election election) {
        election.getMostFittingParticipant().ifPresentOrElse(participant -> {
            if (nodeName.equals(participant)) {
                new Thread(() -> {
                    var project    = orchestrator.getProjectUnsafe(election.getProjectId());
                    var definition = project.map(Project::getPipelineDefinition);
                    var exclusive  = project.flatMap(orchestrator::getPipelineExclusive);

                    exclusive.ifPresentOrElse(
                            container -> {
                                var lock = container.getLock();
                                try (lock) {
                                    var pipeline = container.get().get();
                                    if (orchestrator.startNextStageIfReady(lock, definition.get(), pipeline)) {
                                        container.update(pipeline);
                                    }
                                } catch (LockException | IOException e) {
                                    LOG.log(Level.SEVERE, "Failed to start next stage", e);
                                }
                            },
                            () -> LOG.severe(
                                    "Failed to lock project which should be executed by this node by election")
                    );
                }).start();
            }
        }, () -> {
            if (nodeName.equals(election.getIssuer())) {
                new Thread(() -> {
                    var project   = orchestrator.getProjectUnsafe(election.getProjectId());
                    var exclusive = project.flatMap(orchestrator::getPipelineExclusive);

                    exclusive.ifPresentOrElse(
                            container -> {
                                var lock = container.getLock();
                                try (lock) {
                                    var pipeline = container.get().get();
                                    pipeline.requestPause(Pipeline.PauseReason.NoFittingNodeFound);
                                    container.update(pipeline);
                                } catch (LockException | IOException e) {
                                    LOG.log(Level.SEVERE, "Failed to pause pipeline", e);
                                }
                            },
                            () -> LOG.severe(
                                    "Failed to lock project to note resource exhaustion after Election of which me was the issuer")
                    );
                }).start();
            }
        });
    }
}
