package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.DeletionPolicy;
import de.itdesigners.winslow.api.project.LogEntry;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.api.project.Stats;
import de.itdesigners.winslow.asblr.*;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageDefinitionBuilder;
import de.itdesigners.winslow.fs.*;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.project.LogReader;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Orchestrator {

    private static final Logger  LOG                     = Logger.getLogger(Orchestrator.class.getSimpleName());
    private static final Pattern INVALID_NOMAD_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-_]");
    private static final Pattern MULTI_UNDERSCORE        = Pattern.compile("_[_]+");
    public static final  Pattern PROGRESS_HINT_PATTERN   = Pattern.compile("(([\\d]+[.])?[\\d]+)[ ]*%");

    @Nonnull private final LockBus            lockBus;
    @Nonnull private final Environment        environment;
    @Nonnull private final Backend            backend;
    @Nonnull private final ProjectRepository  projects;
    @Nonnull private final PipelineRepository pipelines;
    @Nonnull private final RunInfoRepository  hints;
    @Nonnull private final LogRepository      logs;
    @Nonnull private final SettingsRepository settings;
    @Nonnull private final String             nodeName;

    @Nonnull private final Map<String, Executor>     executors         = new ConcurrentHashMap<>();
    @Nonnull private final Set<String>               missingResources  = new ConcurrentSkipListSet<>();
    @Nonnull private final DelayedExecutor           delayedExecutions = new DelayedExecutor();
    @Nonnull private final ResourceAllocationMonitor monitor;
    @Nonnull private final ElectionManager           electionManager;

    private boolean executeStages;


    public Orchestrator(
            @Nonnull LockBus lockBus,
            @Nonnull Environment environment,
            @Nonnull Backend backend,
            @Nonnull ProjectRepository projects,
            @Nonnull PipelineRepository pipelines,
            @Nonnull RunInfoRepository hints,
            @Nonnull LogRepository logs,
            @Nonnull SettingsRepository settings,
            @Nonnull String nodeName,
            @Nonnull ResourceAllocationMonitor monitor,
            boolean executeStages) {
        this.lockBus       = lockBus;
        this.environment   = environment;
        this.backend       = backend;
        this.projects      = projects;
        this.pipelines     = pipelines;
        this.hints         = hints;
        this.logs          = logs;
        this.settings      = settings;
        this.nodeName      = nodeName;
        this.monitor       = monitor;
        this.executeStages = executeStages;

        this.electionManager = new ElectionManager(lockBus);

        if (executeStages) {
            this.lockBus.registerEventListener(Event.Command.KILL, this::handleKillEvent);
            this.lockBus.registerEventListener(Event.Command.STOP, this::handleStopEvent);
            this.lockBus.registerEventListener(Event.Command.RELEASE, this::handleReleaseEvent);

            LockBusElectionManagerAdapter.setupAdapters(nodeName, electionManager, this, lockBus);

            this.pollAllPipelinesForUpdate();
        }
    }

    @Nonnull
    public Backend getBackend() {
        return backend;
    }

    @Nonnull
    Election.Participation judgeParticipationScore(@Nonnull ResourceAllocationMonitor.Set<Long> required) {
        return new Election.Participation(
                monitor.getAffinity(required),
                monitor.getAversion(required)
        );
    }

    @Nonnull
    Optional<Project> getProjectUnsafe(@Nonnull String projectId) {
        return projects.getProject(projectId).unsafe();
    }

    @Nonnull
    Optional<LockedContainer<Pipeline>> getPipelineExclusive(@Nonnull Project project) {
        return this.pipelines.getPipeline(project.getId()).exclusive();
    }

    @Nonnull
    Optional<Pipeline> getPipelineUnsafe(@Nonnull String projectId) {
        return pipelines.getPipeline(projectId).unsafe();
    }

    @Nonnull
    ResourceAllocationMonitor.Set<Long> getRequiredResources(@Nonnull EnqueuedStage enqueued) {
        return getRequiredResources(enqueued.getDefinition());
    }

    @Nonnull
    ResourceAllocationMonitor.Set<Long> getRequiredResources(@Nonnull Stage stage) {
        return getRequiredResources(stage.getDefinition());
    }

    @Nonnull
    ResourceAllocationMonitor.Set<Long> getRequiredResources(@Nonnull StageDefinition definition) {
        return definition
                .getRequirements()
                .map(this::toResourceSet)
                .orElseGet(ResourceAllocationMonitor.Set::new);
    }

    @Nonnull
    public Optional<Stats> getRunningStageStats(@Nonnull Project project) {
        return getPipelineUnsafe(project.getId())
                .flatMap(Pipeline::getRunningStage)
                .map(Stage::getId)
                .flatMap(getRunInfoRepository()::getStatsIfStillRelevant);
    }

    private void handleReleaseEvent(@Nonnull Event event) {
        var project = this.projects
                .getProjectIdForLockSubject(event.getSubject())
                .or(() -> this.logs.getProjectIdForLogPath(Path.of(event.getSubject())));
        if (project.isPresent()) {
            LOG.info("Going to check project for changes: " + project.get());
            this.missingResources.remove(project.get());
            this.delayedExecutions.executeRandomlyDelayed(project.get(), 10, 100, () -> {
                this.pollPipelineForUpdate(project.get());
            });
        }
    }

    private void handleStopEvent(@Nonnull Event event) {
        var executor = this.executors.get(event.getSubject());
        if (null != executor) {
            try {
                executor.logErr("Received STOP signal");
                this.backend.stop(event.getSubject());
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to request the backend to stop stage " + event.getSubject(), e);
            }
        }
    }

    private void handleKillEvent(@Nonnull Event event) {
        var executor = this.executors.remove(event.getSubject());
        if (null != executor) {
            try {
                executor.logErr("Received KILL signal");
                this.backend.kill(event.getSubject());
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to request the backend to kill stage " + event.getSubject(), e);
            } finally {
                new Thread(() -> {
                    LockBus.ensureSleepMs(30_000);
                    if (executor.isRunning()) {
                        executor.logErr("Timeout reached: going to stop running executor");
                        this.updatePipeline(executor.getPipeline(), pipeline -> {
                            if (pipeline.getRunningStage().map(Stage::getId).equals(Optional.of(executor.getStage()))) {
                                pipeline.finishRunningStage(State.Failed);
                            }
                        });
                        LockBus.ensureSleepMs(5_000);
                        executor.fail();
                    }
                }).start();
            }
        }
    }

    public void stop(@Nonnull Stage stage) throws LockException {
        this.lockBus.publishCommand(Event.Command.STOP, stage.getId());
    }

    public void kill(@Nonnull Stage stage) throws LockException {
        this.lockBus.publishCommand(Event.Command.KILL, stage.getId());
    }

    @Nonnull
    public RunInfoRepository getRunInfoRepository() {
        return hints;
    }

    @Nonnull
    public LogRepository getLogRepository() {
        return logs;
    }

    private void pollAllPipelinesForUpdate() {
        this.checkPipelinesForUpdate(this.pipelines.getAllPipelines());
    }

    private void pollPipelineForUpdate(@Nonnull String id) {
        this.checkPipelinesForUpdate(Stream.of(this.pipelines.getPipeline(id)));
    }

    private void checkPipelinesForUpdate(@Nonnull Stream<BaseRepository.Handle<Pipeline>> pipelines) {
        if (this.executeStages) {
            pipelines
                    .filter(this::pipelineUpdatable)
                    .map(BaseRepository.Handle::exclusive)
                    .flatMap(Optional::stream)
                    .forEach(this::updatePipeline);
        }
    }

    private boolean pipelineUpdatable(@Nonnull BaseRepository.Handle<Pipeline> handle) {
        try {
            var locked       = handle.isLocked();
            var pipe         = handle.unsafe();
            var projectId    = pipe.map(Pipeline::getProjectId);
            var hasElection  = projectId.flatMap(electionManager::getElection).isPresent();
            var project      = projectId.map(projects::getProject).flatMap(BaseRepository.Handle::unsafe);
            var definition   = project.map(Project::getPipelineDefinition);
            var hasUpdate    = pipe.map(this::isStageStateUpdateAvailable).orElse(false);
            var inconsistent = pipe.map(this::needsConsistencyUpdate).orElse(false);
            var capable      = pipe.flatMap(this::isCapableOfExecutingNextStage).orElse(false);
            var hasResources = pipe.flatMap(this::hasResourcesForNextStage).orElse(false);
            var hasNext = pipe.flatMap(p -> definition.map(d -> maybeEnqueueNextStageOfPipeline(d, p)))
                              .orElse(false);

            LOG.info("Checking, locked=" + locked + ", hasElection=" + hasElection + ", hasUpdate=" + hasUpdate + ", capable=" + capable + ", hasResources=" + hasResources + ", inconsistent=" + inconsistent + ", hasNext=" + hasNext + " projectId=" + projectId);

            if (!locked && !hasElection && hasUpdate && capable && !hasResources) {
                // just missing free resources for now
                this.missingResources.add(pipe.get().getProjectId());
            }

            return !locked && !hasElection && ((hasUpdate) || inconsistent || hasNext);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Failed to poll for " + handle.unsafe().map(Pipeline::getProjectId), t);
            return false;
        }
    }

    @Nonnull
    protected Optional<Boolean> hasResourcesForNextStage(@Nonnull Pipeline pipeline) {
        return pipeline
                .peekNextStage()
                .map(this::getRequiredResources)
                .map(this.monitor::couldReserveConsideringReservations)
                .map(has -> has || pipeline.peekNextStage().map(s -> s.getAction() == Action.Configure).orElse(false));
    }

    @Nonnull
    private ResourceAllocationMonitor.Set<Long> toResourceSet(@Nonnull Requirements requirements) {
        return new ResourceAllocationMonitor.Set<Long>()
                .with(
                        ResourceAllocationMonitor.StandardResources.CPU,
                        (long) requirements.getCpu()
                )
                .with(
                        ResourceAllocationMonitor.StandardResources.RAM,
                        requirements.getMegabytesOfRam() * 1024 * 1024
                )
                .with(
                        ResourceAllocationMonitor.StandardResources.GPU,
                        requirements
                                .getGpu()
                                .map(Requirements.Gpu::getCount)
                                .map(Number::longValue)
                                .orElse(0L)
                );
    }

    protected Optional<Boolean> isCapableOfExecutingNextStage(@Nonnull Pipeline pipeline) {
        return pipeline.peekNextStage().map(enqueued -> {
            switch (enqueued.getAction()) {
                case Execute:
                    return this.backend.isCapableOfExecuting(enqueued.getDefinition());
                default:
                    LOG.warning("Unexpected Action for enqueued Stage: " + enqueued.getAction());
                case Configure:
                    return true;
            }
        });
    }

    private void updatePipeline(@Nonnull LockedContainer<Pipeline> container) {
        var projectId = container.getNoThrow().map(Pipeline::getProjectId);
        var project   = projectId.flatMap(id -> projects.getProject(id).unsafe());
        var pipeline  = Optional.<Pipeline>empty();

        try (container) {
            if (project.isPresent()) {
                updatePipeline(project.get().getPipelineDefinition(), container);
                pipeline = container.get();
            } else {
                LOG.warning("Failed to load project for project " + projectId + ", " + container.getLock());
            }

            pipeline.ifPresent(pipe -> {
                if (pipe.getRunningStage().isEmpty() && isStageStateUpdateAvailable(pipe)) {
                    new Thread(() -> {
                        try {
                            var duration = 2_000L;
                            var puffer   = 100L;
                            if (electionManager.maybeStartElection(pipe.getProjectId(), duration + puffer)) {
                                LockBus.ensureSleepMs(duration);
                                electionManager.closeElection(pipe.getProjectId());
                            }
                        } catch (LockException | IOException e) {
                            LOG.log(Level.SEVERE, "Failed to start election for project " + pipe.getProjectId());
                        }
                    }).start();
                }
            });
        } catch (OrchestratorException | LockException e) {
            LOG.log(
                    Level.SEVERE,
                    "Failed to update pipeline " + container.getNoThrow().map(Pipeline::getProjectId),
                    e
            );
        }

    }

    private void updatePipeline(
            @Nonnull PipelineDefinition definition,
            @Nonnull LockedContainer<Pipeline> container) throws OrchestratorException {
        try {
            var pipelineOpt = container.get();
            if (pipelineOpt.isPresent()) {
                var pipeline = tryUpdateContainer(container, updateRunningStage(pipelineOpt.get()));
                if (this.executeStages && hasNoRunningStage(pipeline)) {
                    var enqueued = maybeEnqueueNextStageOfPipeline(definition, pipeline);
                    //var started  = startNextStageIfReady(container.getLock(), definition, pipeline);
                    if (enqueued/* || started*/) {
                        tryUpdateContainer(container, pipeline);
                    }
                }
            }
        } catch (LockException e) {
            LOG.log(Level.SEVERE, "Failed to get pipeline for update", e);
        }
    }

    private static boolean hasNoRunningStage(Pipeline pipeline) {
        return pipeline
                .getRunningStage()
                .map(Stage::getState)
                .orElse(null) != State.Running;
    }

    private boolean maybeEnqueueNextStageOfPipeline(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) {
        var noneRunning = pipeline.getRunningStage().isEmpty();
        var paused      = pipeline.isPauseRequested();
        var hasNext     = pipeline.peekNextStage().isPresent();
        var successful = pipeline
                .getMostRecentStage()
                .filter(stage -> stage.getAction() == Action.Execute)
                .map(Stage::getState)
                .map(state -> state == State.Succeeded)
                .orElse(Boolean.FALSE);

        if (noneRunning && !paused && !hasNext && successful) {
            return pipeline
                    .getMostRecentStage()
                    .flatMap(_recent -> getNextStageIndex(definition, _recent).map(index -> {
                        var base = definition.getStages().get(index);
                        var env  = new TreeMap<>(base.getEnvironment());

                        var builder = new StageDefinitionBuilder()
                                .withTemplateBase(base)
                                .withEnvironment(env);

                        // overwrite StageDefinition if there is already an
                        // execution instance of it
                        pipeline
                                .getAllStages()
                                .filter(stage -> stage.getFinishState().equals(Optional.of(State.Succeeded)))
                                .filter(stage -> stage.getDefinition().getName().equals(base.getName()))
                                .reduce((first, second) -> second)
                                .ifPresent(recent -> {
                                    env.clear();
                                    env.putAll(recent.getEnv());
                                    recent.getDefinition().getImage().ifPresent(builder::withImage);
                                });


                        var stageId         = "no-id-because-probing-execution-" + System.nanoTime();
                        var stageDefinition = builder.build();
                        var enqueuedStage   = new EnqueuedStage(stageDefinition, Action.Execute);

                        try {
                            // check whether assembling the stage would be possible
                            new StageAssembler()
                                    .add(new EnvironmentVariableAppender(settings.getGlobalEnvironmentVariables()))
                                    .add(new DockerImageAppender())
                                    .add(new EnvLogger())
                                    .add(new UserInputChecker())
                                    .assemble(new Context(
                                            pipeline,
                                            definition,
                                            null,
                                            enqueuedStage,
                                            stageId,
                                            new Submission(stageId, enqueuedStage.getAction(), stageDefinition),
                                            pipeline.getStageCounter() + 1
                                    ));

                            // enqueue the stage only if the execution would be possible
                            pipeline.enqueueStage(stageDefinition);
                            return Boolean.TRUE;
                        } catch (UserInputChecker.MissingUserConfirmationException e) {
                            pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                            return Boolean.TRUE;

                        } catch (UserInputChecker.FurtherUserInputRequiredException e) {
                            pipeline.requestPause(Pipeline.PauseReason.FurtherInputRequired);
                            return Boolean.TRUE;
                        } catch (Throwable t) {
                            LOG.log(
                                    Level.WARNING,
                                    "Unexpected error when checking whether to enqueue the next stage automatically",
                                    t
                            );
                            pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                            return Boolean.TRUE;
                        }
                    })).orElse(Boolean.FALSE);
        } else {
            return false;
        }
    }

    private Optional<Integer> getNextStageIndex(@Nonnull PipelineDefinition definition, Stage recent) {
        var index     = -1;
        var increment = recent.getAction() == Action.Configure ? 0 : 1;

        for (int i = 0; i < definition.getStages().size(); ++i) {
            if (definition.getStages().get(i).getName().equals(recent.getDefinition().getName())) {
                index = i;
                break;
            }
        }

        if (index >= 0 && index + increment < definition.getStages().size()) {
            return Optional.of(index + increment);
        } else {
            return Optional.empty();
        }
    }

    protected boolean startNextStageIfReady(
            @Nonnull Lock lock,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) {
        var noneRunning  = pipeline.getRunningStage().isEmpty();
        var paused       = pipeline.isPauseRequested();
        var hasNext      = pipeline.peekNextStage().isPresent();
        var isCapable    = isCapableOfExecutingNextStage(pipeline).orElse(false);
        var hasResources = hasResourcesForNextStage(pipeline).orElse(false);

        if (noneRunning && !paused && hasNext && isCapable && hasResources) {
            switch (pipeline.getStrategy()) {
                case MoveForwardOnce:
                    pipeline.requestPause();
                case MoveForwardUntilEnd:
                    if (startNextPipelineStage(lock, definition, pipeline)) {
                        setupResourceAllocationAndFreeingListener(pipeline);
                        pipeline.clearPauseReason();
                        return true;
                    }
            }
        }
        return false;
    }

    private void setupResourceAllocationAndFreeingListener(@Nonnull Pipeline pipeline) {
        pipeline.getRunningStage().ifPresent(stage -> {
            var requiredResources = getRequiredResources(stage);
            var executor          = this.executors.get(stage.getId());
            if (executor != null) {
                this.monitor.reserve(requiredResources);
                executor.addShutdownCompletedListener(() -> {
                    this.monitor.free(requiredResources);
                    var copy = new HashSet<>(this.missingResources);
                    this.missingResources.removeAll(copy);
                    copy.forEach(this::pollPipelineForUpdate);
                });
            }
        });
    }

    private boolean startNextPipelineStage(
            @Nonnull Lock lock,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) {
        var nextStage = pipeline.popNextStage();

        if (nextStage.isEmpty()) {
            LOG.warning("Got commanded to start next stage but there is none!");
            return false;
        }

        var stageEnqueued = nextStage.get();
        var stageNumber   = pipeline.getStageCounter() + 1;
        var stageId       = getStageId(pipeline, stageNumber, stageEnqueued.getDefinition());
        var executor      = (Executor) null;

        try {
            final var env = settings.getGlobalEnvironmentVariables();
            final var exec = executor = startExecutor(
                    pipeline.getProjectId(),
                    stageId,
                    getProgressHintMatcher(stageId)
            );

            final var stage = new Stage(
                    stageId,
                    stageEnqueued.getDefinition(),
                    stageEnqueued.getAction(),
                    null
            );

            startStageAssembler(lock, definition, pipeline.clone(), stageEnqueued, stageId, exec, env, stageNumber);

            pipeline.resetResumeNotification();
            pipeline.pushStage(stage);

            return true;
        } catch (LockException | IOException e) {
            LOG.log(Level.SEVERE, "Failed to start next stage of pipeline " + pipeline.getProjectId(), e);
            pipeline.finishRunningStage(State.Failed);
            cleanupOnAssembleError(pipeline.getProjectId(), stageId, executor);
            return true;
        }
    }

    private void startStageAssembler(
            @Nonnull Lock lock,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline,
            @Nonnull EnqueuedStage stageEnqueued,
            @Nonnull String stageId,
            @Nonnull Executor executor,
            @Nonnull Map<String, String> globalEnvironmentVariables,
            int stageNumber) {
        new Thread(() -> {
            var projectId = pipeline.getProjectId();
            var assembler = new StageAssembler();

            try {
                try {
                    assembler
                            .add(new EnvironmentVariableAppender(globalEnvironmentVariables))
                            .add(new DockerImageAppender())
                            .add(new EnvLogger())
                            .add(new UserInputChecker())
                            .add(new WorkspaceCreator(this, environment))
                            .add(new NfsWorkspaceMount((NfsWorkDirectory) environment.getWorkDirectoryConfiguration()))
                            .add(new EnvLogger())
                            .add(new BuildAndSubmit(this.backend, this.nodeName, result -> {
                                executor.setStageHandle(result.getHandle());
                                lock.waitForRelease();
                                updatePipeline(projectId, pipelineToUpdate -> {
                                    pipelineToUpdate.updateStage(result.getStage());
                                });
                            }))
                            .assemble(new Context(
                                    pipeline,
                                    definition,
                                    executor,
                                    stageEnqueued,
                                    stageId,
                                    new Submission(stageId, stageEnqueued.getAction(), stageEnqueued.getDefinition()),
                                    stageNumber
                            ));
                } finally {
                    lock.waitForRelease();
                }

            } catch (UserInputChecker.MissingUserInputException e) {
                cleanupOnAssembleError(projectId, stageId, executor);
                updatePipeline(projectId, toUpdate -> {
                    toUpdate.requestPause(Pipeline.PauseReason.FurtherInputRequired);
                });
            } catch (UserInputChecker.MissingUserConfirmationException e) {
                cleanupOnAssembleError(projectId, stageId, executor);
                updatePipeline(projectId, toUpdate -> {
                    toUpdate.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                });
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to start next stage of pipeline " + projectId, t);
                updatePipeline(projectId, pipelineToUpdate -> {
                    pipelineToUpdate.finishRunningStage(State.Failed);
                });
                cleanupOnAssembleError(projectId, stageId, executor);
            }
        }).start();
    }

    private void cleanupOnAssembleError(
            @Nonnull String projectId,
            @Nonnull String stageId,
            @Nullable Executor executor) {
        if (executor != null) {
            executor.logErr("Assembly failed");
            executor.stop();
        }
        try {
            this.backend.delete(projectId, stageId);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Force purging on the Backend failed", ex);
        }
    }

    @Nonnull
    private Pipeline updateRunningStage(@Nonnull Pipeline pipeline) {
        pipeline.getRunningStage().ifPresent(stage -> {
            LOG.info("Checking if running stage state can be updated: " + getStateOmitExceptions(
                    pipeline,
                    stage
            ) + " for " + stage.getId());
            Supplier<State> finishStateOrFailed = () -> stage.getFinishState().orElse(State.Failed);
            switch (getStateOmitExceptions(pipeline, stage).orElseGet(finishStateOrFailed)) {
                case Running:
                    if (getLogRedirectionState(pipeline) != SimpleState.Failed) {
                        break;
                    }
                default:
                case Failed:
                    stage.finishNow(State.Failed);
                    pipeline.pushStage(null);
                    pipeline.requestPause(Pipeline.PauseReason.StageFailure);
                    try {
                        LOG.info("Killing failed stage");
                        backend.kill(stage.getId());
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to request kill failed stage: " + stage.getId(), e);
                    }
                    break;
                case Succeeded:
                    stage.finishNow(State.Succeeded);
                    pipeline.pushStage(null);
                    break;
            }
        });
        return pipeline;
    }

    @Nonnull
    private Optional<State> getStateOmitExceptions(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        try {
            return getState(pipeline, stage);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to retrieve stage state", e);
            return Optional.empty();
        }
    }

    @Nonnull
    private Optional<State> getState(
            @Nonnull Pipeline pipeline,
            @Nonnull Stage stage) throws IOException {
        // faster & cheaper than potentially causing a REST request on a new TcpConnection
        if (this.executors.get(stage.getId()) != null) {
            return Optional.of(State.Running);
        }
        return backend.getState(pipeline.getProjectId(), stage.getId());
    }

    private boolean needsConsistencyUpdate(@Nonnull Pipeline pipeline) {
        return hasLogRedirectionFailed(pipeline) || unnoticedFinished(pipeline);
    }

    private boolean unnoticedFinished(@Nonnull Pipeline pipeline) {
        var stillMarkedAsRunning = pipeline.getRunningStage().isPresent();
        return getLogRedirectionState(pipeline) != SimpleState.Running && stillMarkedAsRunning;
    }

    private boolean hasLogRedirectionFailed(@Nonnull Pipeline pipeline) {
        return getLogRedirectionState(pipeline) == SimpleState.Failed;
    }

    private SimpleState getLogRedirectionState(Pipeline pipeline) {
        var running = pipeline.getRunningStage();
        if (running.isEmpty()) {
            return SimpleState.Succeeded;
        } else {
            var stage     = running.get();
            var projectId = pipeline.getProjectId();
            var stageId   = stage.getId();

            if (hints.hasLogRedirectionCompletedSuccessfullyHint(stageId)) {
                return SimpleState.Succeeded;
            } else if (!logs.isLocked(projectId, stageId)) {
                LOG.warning("Detected log redirect which has been aborted! " + stageId + "@" + projectId);
                return SimpleState.Failed;
            } else {
                return SimpleState.Running;
            }
        }
    }

    private Consumer<LogEntry> getProgressHintMatcher(@Nonnull String stageId) {
        return entry -> {
            var matcher = PROGRESS_HINT_PATTERN.matcher(entry.getMessage());
            if (matcher.find()) {
                this.hints.setProgressHint(stageId, Math.round(Float.parseFloat(matcher.group(1))));
                LOG.finest(() -> "ProgressHint match: " + matcher.group(1));
            }
        };
    }

    protected boolean isStageStateUpdateAvailable(Pipeline pipeline) {
        var hasEnqueuedStages = pipeline.hasEnqueuedStages();
        var isPaused          = pipeline.isPauseRequested();
        var isRunning = getLogRedirectionState(pipeline) == SimpleState.Running
                || pipeline
                .getRunningStage()
                .flatMap(stage -> getStateOmitExceptions(pipeline, stage))
                .map(state -> State.Running == state)
                .orElse(false);

        return hasEnqueuedStages && !isPaused && !isRunning;
    }

    public boolean deletePipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.pipelines.getPipeline(project.getId()).unsafe().isEmpty()) {
            throw new PipelineNotFoundException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            var pipeline = container.get().orElseThrow(() -> new PipelineNotFoundException(project));

            if (pipeline.getRunningStage().isPresent()) {
                throw new OrchestratorException(
                        "Pipeline is still running. Deleting a running Pipeline is not (yet) supported");
            }

            var workspace = environment.getResourceManager().getWorkspace(getWorkspacePathForPipeline(pipeline));
            workspace.ifPresent(w -> environment
                    .getResourceManager()
                    .getWorkspacesDirectory()
                    .ifPresent(wd -> forcePurgeNoThrows(wd, w))
            );
            return workspace.isPresent() && container.deleteOmitExceptions();

        } catch (LockException e) {
            throw new OrchestratorException("Failed to maintain lock", e);
        }
    }

    public void forcePurgeNoThrows(@Nonnull Path mustBeWithin, @Nonnull Path directory) {
        try {
            forcePurge(environment.getWorkDirectoryConfiguration().getPath(), mustBeWithin, directory);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to get rid of directory " + directory, e);
        }
    }

    public void forcePurgeWorkspaceNoThrows(@Nonnull String projectId, @Nonnull Path workspace) {
        try {
            forcePurgeWorkspace(projectId, workspace);
        } catch (IOException e) {
            LOG.log(
                    Level.SEVERE,
                    "Failed to get rid of workspace directory[" + workspace + "] of Project " + projectId,
                    e
            );
        }
    }

    public void forcePurgeWorkspace(@Nonnull String projectId, @Nonnull Path workspace) throws IOException {
        var scope = this.environment
                .getResourceManager()
                .getWorkspace(getWorkspacePathForPipeline(projectId))
                .orElseThrow(() -> new IOException("Failed to determine scope for ProjectId " + projectId));
        Orchestrator.forcePurge(environment.getWorkDirectoryConfiguration().getPath(), scope, workspace);
    }

    public static void forcePurge(
            @Nonnull Path workDirectory,
            @Nonnull Path mustBeWithin,
            @Nonnull Path path) throws IOException {
        ensurePathToPurgeIsValid(workDirectory, mustBeWithin, path);
        var maxRetries = 3;
        for (int i = 0; i < maxRetries && path.toFile().exists(); ++i) {
            var index = i;
            try (var stream = Files.walk(path)) {
                stream.forEach(entry -> {
                    try {
                        Files.deleteIfExists(entry);
                    } catch (NoSuchFileException ignored) {
                    } catch (IOException e) {
                        if (index + 1 == maxRetries) {
                            throw new RuntimeException("Failed to delete: " + entry, e);
                        }
                    }
                });
            } catch (RuntimeException re) {
                throw new IOException(re);
            }
        }
        Files.deleteIfExists(path);
    }

    public static void ensurePathToPurgeIsValid(
            @Nonnull Path workDirectory,
            @Nonnull Path scope,
            @Nonnull Path path) throws IOException {
        if (!path.normalize().equals(path)) {
            throw new IOException("Path not normalized properly: " + path);
        }

        if (!scope.normalize().equals(scope)) {
            throw new IOException("Scope not normalized properly: " + path);
        }

        if (!path.startsWith(workDirectory)) {
            throw new IOException("Path[" + path + "] is not within working directory[" + workDirectory + "]");
        }

        if (!path.startsWith(scope)) {
            throw new IOException("Path[" + path + "] is not within scope[" + scope + "]");
        }

        if (workDirectory.getNameCount() + 1 >= path.getNameCount()) {
            //
            // this matches the a path like this:
            //
            // /some/path/on/the/fs/winslow/workspaces/my-pipeline
            // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA               forbidden
            //                                        AAAAAAAAAAAA   fine
            //
            throw new IOException("Path[" + path + "] too close to the working directory[" + workDirectory + "]");
        }
    }

    @Nonnull
    public Pipeline createPipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.pipelines.getPipeline(project.getId()).exists()) {
            throw new PipelineAlreadyExistsException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            Pipeline pipeline = new Pipeline(project.getId());
            tryCreateInitDirectories(pipeline, false);
            updatePipeline(project.getPipelineDefinition(), container);
            tryUpdateContainer(container, pipeline);
            return pipeline;
        }
    }

    private void tryCreateInitDirectories(
            @Nonnull Pipeline pipeline,
            boolean failIfAlreadyExists) throws OrchestratorException {
        environment
                .getResourceManager()
                .createWorkspace(WorkspaceCreator.getInitWorkspacePath(pipeline.getProjectId()), failIfAlreadyExists)
                .orElseThrow(() -> new OrchestratorException("Failed to create init directory " + pipeline.getProjectId()));

        environment
                .getResourceManager()
                .createWorkspace(WorkspaceCreator.getPipelineInputPathOf(pipeline), failIfAlreadyExists)
                .orElseThrow(() -> new OrchestratorException("Failed to create init directory " + pipeline.getProjectId()));

        environment
                .getResourceManager()
                .createWorkspace(WorkspaceCreator.getPipelineOutputPathOf(pipeline), failIfAlreadyExists)
                .orElseThrow(() -> new OrchestratorException("Failed to create init directory " + pipeline.getProjectId()));
    }

    private Pipeline tryUpdateContainer(
            @Nonnull LockedContainer<Pipeline> container,
            @Nonnull Pipeline pipeline) throws OrchestratorException {
        try {
            container.update(pipeline);
            return pipeline;
        } catch (IOException e) {
            throw new OrchestratorException("Failed to update pipeline", e);
        }
    }

    private LockedContainer<Pipeline> exclusivePipelineContainer(@Nonnull Project project) throws OrchestratorException {
        return this.pipelines
                .getPipeline(project.getId())
                .exclusive()
                .orElseThrow(() -> new OrchestratorException("Failed to access new pipeline exclusively"));
    }

    private static Path getWorkspacePathForPipeline(@Nonnull Pipeline pipeline) {
        return getWorkspacePathForPipeline(pipeline.getProjectId());
    }

    private static Path getWorkspacePathForPipeline(@Nonnull String projectId) {
        return Path.of(projectId);
    }

    private static String getStageId(@Nonnull Pipeline pipeline, int stageNumber, @Nonnull StageDefinition stage) {
        return replaceInvalidCharactersInJobName(String.format(
                "%s_%04d_%s",
                pipeline.getProjectId(),
                stageNumber,
                stage.getName()
        ));
    }

    @Nonnull
    public Optional<Pipeline> getPipeline(@Nonnull Project project) {
        return getPipeline(project.getId());
    }

    @Nonnull
    private Optional<Pipeline> getPipeline(@Nonnull String projectId) {
        return pipelines.getPipeline(projectId).unsafe();
    }

    @Nonnull
    public <T> Optional<T> updatePipeline(
            @Nonnull Project project,
            @Nonnull Function<Pipeline, T> updater) {
        return updatePipeline(project.getId(), updater);
    }

    private void updatePipeline(
            @Nonnull String projectId,
            @Nonnull Consumer<Pipeline> updater) {
        this.updatePipeline(projectId, pipeline -> {
            updater.accept(pipeline);
            return null;
        });
    }

    @Nonnull
    private <T> Optional<T> updatePipeline(
            @Nonnull String projectId,
            @Nonnull Function<Pipeline, T> updater) {
        return pipelines.getPipeline(projectId).exclusive().flatMap(container -> {
            try (container) {
                var result   = Optional.<T>empty();
                var pipeline = container.get();
                if (pipeline.isPresent()) {
                    result = Optional.ofNullable(updater.apply(pipeline.get()));
                    container.update(pipeline.get());
                }
                return result;
            } catch (LockException | IOException e) {
                LOG.log(Level.SEVERE, "Failed to update pipeline", e);
                return Optional.empty();
            }
        });
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull String stageId) {
        try {
            return LogReader.stream(logs.getRawInputStreamNonExclusive(project.getId(), stageId));
        } catch (FileNotFoundException e) {
            return Stream.empty();
        }
    }

    private Executor startExecutor(
            @Nonnull String projectId,
            @Nonnull String stage,
            @Nonnull Consumer<LogEntry> consumer) throws LockException, FileNotFoundException {
        var executor = new Executor(projectId, stage, this);
        executor.addShutdownListener(() -> this.executors.remove(stage));
        executor.addShutdownListener(() -> this.cleanupAfterStageExecution(stage));
        executor.addShutdownListener(() -> this.discardObsoleteWorkspaces(projectId));
        executor.addShutdownCompletedListener(() -> this.pollPipelineForUpdate(projectId));
        executor.addLogEntryConsumer(consumer);
        this.executors.put(stage, executor);
        return executor;
    }

    private void cleanupAfterStageExecution(@Nonnull String stageId) {
        try {
            getRunInfoRepository().removeAllProperties(stageId);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to cleanup stage " + stageId, e);
        }
    }

    private void discardObsoleteWorkspaces(@Nonnull String projectId) {
        getPipeline(projectId).ifPresent(pipeline -> {
            var policy = pipeline
                    .getDeletionPolicy()
                    .or(() -> this.projects
                            .getProject(projectId)
                            .unsafe()
                            .map(Project::getPipelineDefinition)
                            .flatMap(PipelineDefinition::getDeletionPolicy)
                    )
                    .orElseGet(Orchestrator::defaultDeletionPolicy);
            var history    = pipeline.getCompletedStages().collect(Collectors.toList());
            var finder     = new ObsoleteWorkspaceFinder(policy).withExecutionHistory(history);
            var obsolete   = finder.collectObsoleteWorkspaces();
            var workspaces = environment.getResourceManager();
            var purgeScope = environment.getResourceManager().getWorkspace(getWorkspacePathForPipeline(pipeline));

            if (purgeScope.isEmpty()) {
                LOG.warning("Cannot determine purge scope for Pipeline with ProjectId " + pipeline.getProjectId());
                return;
            }

            obsolete.stream()
                    .map(Path::of)
                    .map(workspaces::getWorkspace)
                    .flatMap(Optional::stream)
                    .filter(Files::exists)
                    .peek(path -> LOG.info("Deleting obsolete workspace at " + path))
                    .forEach(path -> forcePurgeNoThrows(purgeScope.get(), path));
        });
    }

    /**
     * Deletes any failed stage from the {@link Pipeline} of the given {@link Project}. Also
     * deletes associated workspace and log files.
     *
     * @param project {@link Project} to prune
     * @throws IOException An accumulated exception for every failed stage that failed to be pruned
     */
    public void prunePipeline(@Nonnull Project project) throws IOException {
        var exception = getPipelineExclusive(project).flatMap(container -> {
            try (container; var heart = new LockHeart(container.getLock())) {
                var pipeline = container.getNoThrow();
                if (pipeline.isPresent()) {
                    var pipe   = pipeline.get();
                    var except = Optional.<IOException>empty();
                    var prunable = pipe
                            .getCompletedStages()
                            .filter(s -> s.getState() == State.Failed)
                            .collect(Collectors.toUnmodifiableList());

                    for (var stage : prunable) {
                        try {
                            var path      = stage.getWorkspace().map(Path::of);
                            var workspace = path.flatMap(p -> environment.getResourceManager().getWorkspace(p));
                            if (workspace.isPresent()) {
                                forcePurgeWorkspace(project.getId(), workspace.get());
                            }
                            logs.deleteLogsIfExistsNoThrows(project.getId(), stage.getId());
                            pipe.removeStage(stage.getId());
                            container.update(pipe);
                        } catch (IOException e) {
                            if (except.isEmpty()) {
                                except = Optional.of(e);
                            } else {
                                except.get().addSuppressed(e);
                            }
                        }
                    }

                    return except;
                } else {
                    return Optional.empty();
                }
            }
        });

        if (exception.isPresent()) {
            throw exception.get();
        }
    }

    @Nonnull
    public static DeletionPolicy defaultDeletionPolicy() {
        return new DeletionPolicy(false, null);
    }

    public static String replaceInvalidCharactersInJobName(@Nonnull String jobName) {
        return MULTI_UNDERSCORE
                .matcher(INVALID_NOMAD_CHARACTER.matcher(jobName.toLowerCase()).replaceAll("_"))
                .replaceAll("_");
    }

    private enum SimpleState {
        Running, Failed, Succeeded,
    }
}
