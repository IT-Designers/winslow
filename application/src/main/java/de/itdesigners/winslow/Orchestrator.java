package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;
import de.itdesigners.winslow.asblr.*;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.auth.UserRepository;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.Lock;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.node.NodeRepository;
import de.itdesigners.winslow.pipeline.*;
import de.itdesigners.winslow.project.LogReader;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.resource.ResourceManager;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Orchestrator implements Closeable, AutoCloseable {

    private static final Logger  LOG                   = Logger.getLogger(Orchestrator.class.getSimpleName());
    public static final  Pattern PROGRESS_HINT_PATTERN = Pattern.compile("(([\\d]+[.])?[\\d]+)[ ]*%");
    public static final  Pattern RESULT_PATTERN        = Pattern.compile("WINSLOW_RESULT:[ ]+(.*)=(.*)");

    @Nonnull private final LockBus            lockBus;
    @Nonnull private final Environment        environment;
    @Nonnull private final Backend            backend;
    @Nonnull private final ProjectRepository  projects;
    @Nonnull private final PipelineRepository pipelines;
    @Nonnull private final RunInfoRepository  hints;
    @Nonnull private final LogRepository      logs;
    @Nonnull private final SettingsRepository settings;
    @Nonnull private final UserRepository     users;
    @Nonnull private final NodeRepository     nodes;
    @Nonnull private final String             nodeName;

    @Nonnull private final Map<String, Executor>     executors         = new ConcurrentHashMap<>();
    @Nonnull private final Set<String>               missingResources  = new ConcurrentSkipListSet<>();
    @Nonnull private final DelayedExecutor           delayedExecutions = new DelayedExecutor();
    @Nonnull private final ResourceAllocationMonitor monitor;
    @Nonnull private final ElectionManager           electionManager;

    private final boolean       executeStages;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final @Nonnull Map<String, Queue<Consumer<Pipeline>>> deferredPipelineUpdates = new ConcurrentHashMap<>();
    private final @Nonnull Set<String>                            stageExecutionTags      = new ConcurrentSkipListSet<>();

    public Orchestrator(
            @Nonnull LockBus lockBus,
            @Nonnull Environment environment,
            @Nonnull Backend backend,
            @Nonnull ProjectRepository projects,
            @Nonnull PipelineRepository pipelines,
            @Nonnull RunInfoRepository hints,
            @Nonnull LogRepository logs,
            @Nonnull SettingsRepository settings,
            @Nonnull UserRepository users,
            @Nonnull NodeRepository nodes,
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
        this.users         = users;
        this.nodes         = nodes;
        this.nodeName      = nodeName;
        this.monitor       = monitor;
        this.executeStages = executeStages;

        this.electionManager = new ElectionManager(lockBus);

        this.stageExecutionTags.add("winslow:node:" + this.nodeName);
        this.stageExecutionTags.add("winslow:server:" + this.nodeName);
    }

    public void start() {
        if (executeStages) {
            if (!started.getAndSet(true)) {
                this.lockBus.registerEventListener(Event.Command.KILL, this::handleKillEvent);
                this.lockBus.registerEventListener(Event.Command.STOP, this::handleStopEvent);
                this.lockBus.registerEventListener(Event.Command.RELEASE, this::handleReleaseEvent);

                LockBusElectionManagerAdapter.setupAdapters(nodeName, electionManager, this, lockBus);
                this.checkPipelinesForUpdate(this.pipelines.getAllPipelines());

            }
        }
    }

    @Nonnull
    public Backend getBackend() {
        return backend;
    }

    @Nonnull
    public ElectionManager getElectionManager() {
        return electionManager;
    }

    @Nonnull
    public ProjectRepository getProjects() {
        return projects;
    }

    @Nonnull
    public PipelineRepository getPipelines() {
        return pipelines;
    }

    @Nonnull
    public DelayedExecutor getDelayedExecutor() {
        return delayedExecutions;
    }

    @Nonnull
    public RunInfoRepository getRunInfoRepository() {
        return hints;
    }

    @Nonnull
    public LogRepository getLogRepository() {
        return logs;
    }

    @Nonnull
    public ResourceManager getResourceManager() {
        return environment.getResourceManager();
    }

    public void addStageExecutionTag(@Nonnull String tag) {
        this.stageExecutionTags.add(tag);
    }

    @Nonnull
    public Iterable<String> getStageExecutionTags() {
        return this.stageExecutionTags;
    }


    @Nonnull
    Election.Participation judgeParticipationScore(@Nonnull ResourceAllocationMonitor.ResourceSet<Long> required) {
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
    ResourceAllocationMonitor.ResourceSet<Long> getRequiredResources(@Nonnull StageDefinition definition) {
        return definition
                .getRequirements()
                .map(this::toResourceSet)
                .orElseGet(ResourceAllocationMonitor.ResourceSet::new);
    }

    @Nonnull
    public Stream<Stats> getRunningStageStats(@Nonnull Project project) {
        return getPipelineUnsafe(project.getId())
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .flatMap(ExecutionGroup::getRunningStages)
                .map(Stage::getFullyQualifiedId)
                .map(getRunInfoRepository()::getStatsIfStillRelevant)
                .flatMap(Optional::stream);
    }

    private void handleReleaseEvent(@Nonnull Event event) {
        tryTriggerDeferredPipelineUpdates(event.getSubject());

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

    private void tryTriggerDeferredPipelineUpdates(@Nonnull String projectId) {
        Optional.ofNullable(deferredPipelineUpdates.get(projectId))
                .ifPresent(queue -> pipelines
                        .getPipeline(projectId)
                        .exclusive()
                        .ifPresent(container -> {
                            try (container) {
                                var pipeline = container.get();
                                while (!queue.isEmpty() && pipeline.isPresent()) {
                                    var updater = queue.poll();
                                    var pipe    = pipeline.get();

                                    if (updater != null) {
                                        updater.accept(pipe);
                                        container.update(pipe);
                                    }
                                }
                            } catch (LockException | IOException e) {
                                LOG.log(Level.SEVERE, "Failed to update pipeline", e);
                            }
                        }));
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
                executor.kill();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to request the backend to kill stage " + event.getSubject(), e);
            } finally {
                new Thread(() -> {
                    LockBus.ensureSleepMs(30_000);
                    if (executor.isRunning()) {
                        executor.logErr("Timeout reached: going to stop running executor");
                        this.enqueuePipelineUpdate(executor.getPipeline(), pipeline -> {
                            pipeline
                                    .getActiveExecutionGroups()
                                    .forEach(group -> {
                                                 try {
                                                     group.updateStage(
                                                             event.getSubject(),
                                                             stage -> {
                                                                 stage.finishNow(State.Failed);
                                                                 return Optional.of(stage);
                                                             }
                                                     );
                                                 } catch (StageIsArchivedAndNotAllowedToChangeException e) {
                                                     LOG.log(
                                                             Level.FINE,
                                                             "Failed to force abort onto stage " + event.getSubject(),
                                                             e
                                                     );
                                                 }
                                             }
                                    );
                        });
                        LockBus.ensureSleepMs(5_000);
                        executor.fail();
                    }
                }).start();
            }
        }
    }

    public void stop(@Nonnull Stage stage) throws LockException {
        this.lockBus.publishCommand(Event.Command.STOP, stage.getFullyQualifiedId());
    }

    public void kill(@Nonnull Stage stage) throws LockException {
        this.kill(stage.getFullyQualifiedId());
    }

    public void kill(@Nonnull String fullyQualifiedStageId) throws LockException {
        this.lockBus.publishCommand(Event.Command.KILL, fullyQualifiedStageId);
    }

    public void killLocally(@Nonnull String fullyQualifiedStageId) throws IOException {
        this.backend.kill(fullyQualifiedStageId);
    }

    private void pollPipelineForUpdate(@Nonnull String id) {
        this.checkPipelinesForUpdate(Stream.of(this.pipelines.getPipeline(id)));
    }

    private synchronized void checkPipelinesForUpdate(@Nonnull Stream<BaseRepository.Handle<Pipeline>> pipelines) {
        if (this.executeStages) {
            pipelines
                    .filter(h -> !h.isLocked())
                    .flatMap(h -> h.unsafe().map(p -> new PipelineUpdater(this, p, h)).stream())
                    .filter(u -> {
                        u.evaluateUpdatesWithoutExclusivePipelineAccess();
                        return u.hasPipelineUpdatesThatRequireExclusiveAccess();
                    })
                    .forEach(u -> {
                        try {
                            // TODO remove
                            LOG.info("evaluateUpdatesWithExclusivePipelineAccess: " + String.join(
                                    ",",
                                    u.listPipelineUpdates()
                            ));
                            u.evaluateUpdatesWithExclusivePipelineAccess();
                        } catch (LockException | IOException e) {
                            LOG.log(Level.SEVERE, "Failed to update pipeline", e);
                        }
                    });
        }
    }

    public void addProjectThatNeedsToBeReEvaluatedOnceMoreResourcesAreAvailable(@Nonnull String projectId) {
        this.missingResources.add(projectId);
    }

    @Nonnull
    private ResourceAllocationMonitor.ResourceSet<Long> toResourceSet(@Nonnull Requirements requirements) {
        return new ResourceAllocationMonitor.ResourceSet<Long>()
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

    @Nonnull
    public Optional<Boolean> hasResourcesToExecuteNextStage(@Nonnull Pipeline pipeline) {
        return pipeline
                .getActiveOrNextExecutionGroup()
                .filter(executionGroup -> executionGroup.getNextStageDefinition().isPresent())
                .findFirst()
                .map(group -> {
                    if (group.isConfigureOnly()) {
                        return true;
                    }

                    var resources = getRequiredResources(group.getStageDefinition());
                    var wouldExceedLimit = getProjectUnsafe(pipeline.getProjectId()).map(project -> {
                        var allocView = new DistributedAllocationView(project.getOwner(), pipeline.getProjectId());

                        var settingsLimit = settings.getUserResourceLimitations().unsafe();
                        var userLimit     = users.getUser(project.getOwner()).flatMap(User::getResourceLimitation);


                        if (settingsLimit.isPresent() && userLimit.isPresent()) {
                            allocView.setUserLimit(settingsLimit.get().min(userLimit.get()));
                        } else {
                            settingsLimit.or(() -> userLimit).ifPresent(allocView::setUserLimit);
                        }

                        allocView.loadAllocInfo(
                                nodes.loadActiveNodes().flatMap(info -> info.getAllocInfo().stream()),
                                new CachedFunction<>(this::getProjectUnsafe)
                        );
                        return allocView.wouldResourcesExceedLimit(resources);
                    }).orElse(Boolean.FALSE);

                    boolean couldReserve = this.monitor.couldReserveConsideringReservations(resources);
                    boolean result       = !wouldExceedLimit && couldReserve;

                    if (!result) {
                        LOG.info(
                                "hasResourcesToExecuteNextStage('" + pipeline.getProjectId() + "') => false"
                                        + ", wouldExceedLimit=" + wouldExceedLimit
                                        + ", couldReserveConsideringReservations=" + couldReserve
                        );
                    }

                    return result;
                });
    }

    public Optional<Boolean> isCapableOfExecutingNextStage(@Nonnull Pipeline pipeline) {
        return pipeline
                .getActiveOrNextExecutionGroup()
                .filter(executionGroup -> executionGroup.getNextStageDefinition().isPresent())
                .findFirst()
                .map(group -> {
                    if (group.isConfigureOnly()) {
                        return true;
                    }
                    boolean hasAllTags     = this.stageExecutionTags.containsAll(group.getStageDefinition().getTags());
                    boolean backendCapable = this.backend.isCapableOfExecuting(group.getStageDefinition());
                    boolean result         = hasAllTags && backendCapable;

                    if (!result) {
                        LOG.info(
                                "isCapableOfExecutingNextStage('" + pipeline.getProjectId() + "') => false"
                                        + ", hasAllTags=" + hasAllTags
                                        + ", backendCapable=" + backendCapable
                        );
                    }

                    return result;
                });
    }


    protected boolean startPipeline(
            @Nonnull Lock lock,
            @Nonnull String projectId,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) {
        return startNextPipelineStage(lock, definition, pipeline).map(result -> {
            hookUpResourceReservationAndFreeingHandler(
                    projectId,
                    result.getValue0().getStageDefinition(),
                    result.getValue2()
            );
            pipeline.clearPauseReason();
            return Boolean.TRUE;
        }).orElse(Boolean.FALSE);
    }

    private void hookUpResourceReservationAndFreeingHandler(
            @Nonnull String projectId,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Executor executor) {
        var requiredResources = getRequiredResources(stageDefinition);
        this.monitor.reserve(projectId, requiredResources);
        executor.addShutdownCompletedListener(() -> {
            this.monitor.free(projectId, requiredResources);
            takeAllPipelinesThatWereRecordedForMissingResources().forEach(this::pollPipelineForUpdate);
        });
    }

    @Nonnull
    private Set<String> takeAllPipelinesThatWereRecordedForMissingResources() {
        var copy = new HashSet<>(this.missingResources);
        this.missingResources.removeAll(copy);
        return copy;
    }

    private Optional<Triplet<ExecutionGroup, Stage, Executor>> startNextPipelineStage(
            @Nonnull Lock lock,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) {
        if (pipeline.canRetrieveNextActiveExecution()) {
            pipeline.retrieveNextActiveExecution();
        }

        var executionGroup = pipeline
                .getActiveExecutionGroups()
                .filter(g -> g.getNextStageDefinition().isPresent())
                .findFirst();
        var nextStageDefinition = executionGroup.flatMap(ExecutionGroup::getNextStageDefinition);

        if (nextStageDefinition.isEmpty()) {
            LOG.warning("Got commanded to start next stage but there is none!");
            return Optional.empty();
        }

        var       stageId         = nextStageDefinition.get().getValue0();
        var       stageDefinition = nextStageDefinition.get().getValue1();
        var       executor        = (Executor) null;
        final var stage           = new Stage(stageId, null);

        try {
            final var env = settings.getGlobalEnvironmentVariables();

            executor = startExecutor(stageId);

            startStageAssembler(
                    lock,
                    definition,
                    pipeline,
                    executionGroup.get(),
                    stageDefinition,
                    stageId,
                    executor,
                    env
            );
        } catch (LockException | IOException e) {
            LOG.log(Level.SEVERE, "Failed to start next stage of pipeline " + pipeline.getProjectId(), e);
            cleanupOnAssembleError(pipeline.getProjectId(), stageId.getFullyQualified(), executor);
            return Optional.empty();
        }

        // this code cannot fail anymore
        executionGroup.get().addStage(stage);

        return Optional.of(new Triplet<>(executionGroup.get(), stage, executor));
    }

    private void startStageAssembler(
            @Nonnull Lock lock,
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline,
            @Nonnull ExecutionGroup executionGroup,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull StageId stageId,
            @Nonnull Executor executor,
            @Nonnull Map<String, String> globalEnvironmentVariables) {
        var thread = new Thread(() -> {
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
                            .add(new WorkspaceMount(environment.getWorkDirectoryConfiguration()))
                            .add(new EnvLogger())
                            .add(new LogParserRegisterer(getResourceManager()))
                            .add(new BuildAndSubmit(this.backend, this.nodeName, result -> {
                                result.getStage().startNow();
                                executor.setStageHandle(result.getHandle());
                                lock.waitForRelease();
                                // do not update deferred, update as soon as possible!
                                updatePipeline(projectId, pipelineToUpdate -> {
                                    pipelineToUpdate
                                            .getActiveExecutionGroups()
                                            .filter(g -> {
                                                try {
                                                    return g.updateStage(result.getStage());
                                                } catch (StageIsArchivedAndNotAllowedToChangeException e) {
                                                    LOG.log(
                                                            Level.SEVERE,
                                                            "Failed to update stage with assemble result",
                                                            e
                                                    );
                                                    throw new RuntimeException(e); // bubble up
                                                }
                                            })
                                            .findFirst()
                                            .orElseThrow();
                                });
                            }))
                            .assemble(new Context(
                                    pipeline,
                                    definition,
                                    executionGroup,
                                    executor,
                                    stageId,
                                    new Submission(
                                            stageId,
                                            executionGroup.isConfigureOnly(),
                                            stageDefinition,
                                            executionGroup.getWorkspaceConfiguration()
                                    )
                            ));
                } finally {
                    lock.waitForRelease();
                }
            } catch (UserInputChecker.MissingUserInputException e) {
                cleanupOnAssembleError(projectId, stageId.getFullyQualified(), executor);
                enqueuePipelineUpdate(projectId, toUpdate -> {
                    toUpdate.requestPause(Pipeline.PauseReason.FurtherInputRequired);
                });
            } catch (UserInputChecker.MissingUserConfirmationException e) {
                cleanupOnAssembleError(projectId, stageId.getFullyQualified(), executor);
                enqueuePipelineUpdate(projectId, toUpdate -> {
                    toUpdate.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                });
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to start next stage of pipeline " + projectId, t);
                enqueuePipelineUpdate(projectId, pipelineToUpdate -> {
                    pipelineToUpdate.requestPause(Pipeline.PauseReason.StageFailure);
                    long executedUpdates = pipelineToUpdate
                            .getActiveExecutionGroups()
                            .filter(
                                    group -> {
                                        try {
                                            return group.updateStage(
                                                    stageId.getFullyQualified(),
                                                    stage -> {
                                                        stage.finishNow(State.Failed);
                                                        return Optional.of(stage);
                                                    }
                                            );
                                        } catch (StageIsArchivedAndNotAllowedToChangeException e) {
                                            LOG.log(
                                                    Level.SEVERE,
                                                    "Failed to set failed flag for stage " + stageId,
                                                    e
                                            );
                                            return false;
                                        }
                                    })
                            .count();
                    if (executedUpdates != 1) {
                        LOG.log(Level.SEVERE, "Failed to retrieve the active ExecutionGroup for " + projectId);
                    }
                });
                cleanupOnAssembleError(projectId, stageId.getFullyQualified(), executor);
            }
        });
        thread.setName(stageId.getFullyQualified());
        thread.start();
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

    private Consumer<LogEntry> getProgressHintMatcher(@Nonnull String stageId) {
        return entry -> {
            var matcher = PROGRESS_HINT_PATTERN.matcher(entry.getMessage());
            if (matcher.find()) {
                this.hints.setProgressHint(stageId, Math.round(Float.parseFloat(matcher.group(1))));
                LOG.finest(() -> "ProgressHint match: " + matcher.group(1));
            }
        };
    }

    private Consumer<LogEntry> getResultMatcher(@Nonnull String stageId) {
        return entry -> {
            var matcher = RESULT_PATTERN.matcher(entry.getMessage());
            if (matcher.find()) {
                this.hints.setResult(stageId, matcher.group(1), matcher.group(2));
                LOG.finest(() -> "Result match: " + matcher.group(1));
            }
        };
    }

    public boolean deletePipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.pipelines.getPipeline(project.getId()).unsafe().isEmpty()) {
            throw new PipelineNotFoundException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            var pipeline = container.get().orElseThrow(() -> new PipelineNotFoundException(project));

            if (pipeline.getActiveExecutionGroups().flatMap(ExecutionGroup::getRunningStages).findAny().isPresent()) {
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
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
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

        var handle = this.pipelines.getPipeline(project.getId());

        try (var container = handle.exclusive()
                                   .orElseThrow(() -> new OrchestratorException(
                                           "Failed to access new pipeline exclusively"));
             var heart = new LockHeart(container.getLock())) {

            Pipeline pipeline = new Pipeline(project.getId());
            tryCreateInitDirectories(pipeline, false);
            tryUpdateContainer(container, pipeline);

            try {
                new PipelineUpdater(this, pipeline.getProjectId(), handle).evaluate();
            } catch (LockException | IOException e) {
                LOG.log(Level.SEVERE, "Failed to evaluate pipeline updates");
            }

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

    public static Path getWorkspacePathForPipeline(@Nonnull Pipeline pipeline) {
        return getWorkspacePathForPipeline(pipeline.getProjectId());
    }

    private static Path getWorkspacePathForPipeline(@Nonnull String projectId) {
        return Path.of(projectId);
    }

    @Nonnull
    public Optional<Pipeline> getPipeline(@Nonnull Project project) {
        return getPipeline(project.getId());
    }

    @Nonnull
    public Optional<Pipeline> getPipeline(@Nonnull String projectId) {
        return pipelines.getPipeline(projectId).unsafe();
    }

    /**
     * @deprecated This can cause data-races, use {@link #enqueuePipelineUpdate(String, Consumer)} instead
     */
    @Nonnull
    @Deprecated
    public <T> Optional<T> updatePipeline(
            @Nonnull Project project,
            @Nonnull Function<Pipeline, T> updater) {
        return updatePipeline(project.getId(), updater);
    }

    /**
     * @param projectId Id of the {@link Project} to update the {@link Pipeline} for
     * @param updater   {@link Consumer} to invoke to update the {@link Pipeline}
     * @return Whether the {@link Consumer} was invoked
     * @deprecated This can cause data-races, use {@link #enqueuePipelineUpdate(String, Consumer)} instead
     */
    @Deprecated
    private boolean updatePipeline(
            @Nonnull String projectId,
            @Nonnull Consumer<Pipeline> updater) {
        return this.updatePipeline(projectId, pipeline -> {
            updater.accept(pipeline);
            return Boolean.TRUE;
        }).orElse(Boolean.FALSE);
    }

    public void enqueuePipelineUpdate(@Nonnull String projectId, @Nonnull Consumer<Pipeline> update) {
        deferredPipelineUpdates
                .computeIfAbsent(projectId, pid -> new ConcurrentLinkedQueue<>())
                .add(update);
        tryTriggerDeferredPipelineUpdates(projectId);
    }

    /**
     * @deprecated This can cause data-races, use {@link #enqueuePipelineUpdate(String, Consumer)} instead
     */
    @Nonnull
    @Deprecated
    private <T> Optional<T> updatePipeline(
            @Nonnull String projectId,
            @Nonnull Function<Pipeline, T> update) {
        return pipelines
                .getPipeline(projectId)
                .exclusive()
                .flatMap(container -> applyUpdateOnPipelineContainer(container, update));
    }

    @Nonnull
    private <T> Optional<? extends T> applyUpdateOnPipelineContainer(
            LockedContainer<Pipeline> container,
            @Nonnull Function<Pipeline, T> update) {
        try (container) {
            var result   = Optional.<T>empty();
            var pipeline = container.get();
            if (pipeline.isPresent()) {
                result = Optional.ofNullable(update.apply(pipeline.get()));
                container.update(pipeline.get());
            }
            return result;
        } catch (LockException | IOException e) {
            LOG.log(Level.SEVERE, "Failed to update pipeline", e);
            return Optional.empty();
        }
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull StageId id) {
        return this.getLogs(project, id.getFullyQualified());
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull StageId id, long skipBytes) {
        return this.getLogs(project, id.getFullyQualified(), skipBytes);
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull String stageId) {
        return this.getLogs(project.getId(), stageId, 0L);
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull String stageId, long skipBytes) {
        return this.getLogs(project.getId(), stageId, skipBytes);
    }

    @Nonnull
    public Stream<LogEntry> getLogs(@Nonnull String projectId, @Nonnull String stageId) {
        return this.getLogs(projectId, stageId, 0L);
    }

    public Stream<LogEntry> getLogs(@Nonnull String projectId, @Nonnull String stageId, long skipBytes) {
        try {
            return LogReader.stream(logs.getRawInputStreamNonExclusive(projectId, stageId, skipBytes));
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    public long getLogSize(@Nonnull Project project, @Nonnull StageId id) {
        return getLogSize(project, id.getFullyQualified());
    }

    public long getLogSize(@Nonnull Project project, @Nonnull String stageId) {
        return logs.getLogSize(project.getId(), stageId);
    }

    private Executor startExecutor(@Nonnull StageId stageId) throws LockException, FileNotFoundException {
        var executor = new Executor(stageId.getProjectId(), stageId.getFullyQualified(), this);
        executor.addShutdownListener(() -> this.executors.remove(stageId.getFullyQualified()));
        executor.addShutdownCompletedListener(() -> this.pollPipelineForUpdate(stageId.getProjectId()));
        executor.addLogEntryConsumer(getProgressHintMatcher(stageId.getFullyQualified()));
        executor.addLogEntryConsumer(getResultMatcher(stageId.getFullyQualified()));
        this.executors.put(stageId.getFullyQualified(), executor);
        return executor;
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
                            .getActiveAndPastExecutionGroups()
                            .flatMap(g -> g.getStages().map(s -> new Pair<>(g, s)))
                            .filter(s -> s.getValue1().getState() == State.Failed)
                            .collect(Collectors.toUnmodifiableList());


                    for (var pair : prunable) {
                        try {
                            var path      = pair.getValue1().getWorkspace().map(Path::of);
                            var workspace = path.flatMap(p -> environment.getResourceManager().getWorkspace(p));
                            if (workspace.isPresent()) {
                                forcePurgeWorkspace(project.getId(), workspace.get());
                            }
                            logs.deleteLogsIfExistsNoThrows(project.getId(), pair.getValue1().getFullyQualifiedId());
                            assert pair.getValue0().removeStage(pair.getValue1().getFullyQualifiedId());
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

    @Override
    public void close() throws IOException {
        this.backend.close();
    }

    private enum SimpleState {
        Running, Failed, Succeeded,
    }
}
