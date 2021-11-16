package de.itdesigners.winslow;

import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.pipeline.CommonUpdateConstraints;
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
                .filter(CommonUpdateConstraints::hasAnyActiveExecutionGroupRemainingExecutions)
                .filter(pipeline -> !pipeline.isPauseRequested())
                .filter(p -> orchestrator.isCapableOfExecutingNextStage(p).orElse(Boolean.FALSE))
                .filter(p -> {
                    var hasResourced = orchestrator.hasResourcesToExecuteNextStage(p).orElse(Boolean.FALSE);
                    if (!hasResourced) {
                        orchestrator.addProjectThatNeedsToBeReEvaluatedOnceMoreResourcesAreAvailable(p.getProjectId());
                    }
                    return hasResourced;
                })
                .flatMap(pipeline -> pipeline
                        .getActiveOrNextExecutionGroup()
                        .filter(executionGroup -> executionGroup.getNextStageDefinition().isPresent())
                        .findFirst()
                )
                .ifPresent(activeGroup -> {
                    var requiredResources = orchestrator.getRequiredResources(activeGroup.getStageDefinition());
                    var participation     = orchestrator.judgeParticipationScore(requiredResources);

                    try {
                        electionManager.participate(election, participation);
                        election.setRequiredResources(requiredResources);
                        orchestrator.getResourceAllocationMonitor().reserve(
                                election.getProjectId(),
                                requiredResources
                        );
                    } catch (IOException | LockException e) {
                        LOG.log(
                                Level.SEVERE,
                                "Failed to participate in election for project=" + election.getProjectId(),
                                e
                        );
                    }
                });
    }

    private void handleElectionClosed(@Nonnull Election election) {
        if (election.hasParticipated(nodeName)) {
            orchestrator.getResourceAllocationMonitor().free(
                    election.getProjectId(),
                    election.getRequiredResources()
            );
        }

        election.getMostFittingParticipant().ifPresentOrElse(participant -> {
            if (nodeName.equals(participant)) {
                var thread = new Thread(() -> {
                    var projectOpt    = orchestrator.getProjectUnsafe(election.getProjectId());
                    var definitionOpt = projectOpt.map(Project::getPipelineDefinition);
                    var exclusiveOpt  = projectOpt.flatMap(orchestrator::getPipelineExclusive);

                    exclusiveOpt.ifPresentOrElse(
                            container -> {
                                try (var lock = container.getLock()) {
                                    var pipeline  = container.get().get();
                                    var project = projectOpt.get();
                                    var definition = definitionOpt.get();
                                    if (orchestrator.startPipeline(lock, project, definition, pipeline)) {
                                        LOG.info("Updating pipeline...");
                                        container.update(pipeline);
                                    } else {
                                        LOG.info("No pipeline update available...");
                                    }
                                    LOG.info("Closing lock=" + lock);
                                } catch (LockException | IOException e) {
                                    LOG.log(Level.SEVERE, "Failed to start next stage", e);
                                } finally {
                                    LOG.info("Closed lock");
                                }
                            },
                            () -> LOG.severe("Failed to lock project which should be executed by this node by election")
                    );
                });
                thread.setName(election.getProjectId() + ".win");
                thread.start();
            }
        }, () -> {
            if (nodeName.equals(election.getIssuer())) {
                orchestrator.enqueuePipelineUpdate(
                        election.getProjectId(),
                        pipeline -> pipeline.requestPause(Pipeline.PauseReason.NoFittingNodeFound)
                );
            }
        });
    }
}
