package de.itd.tracking.winslow;

import de.itd.tracking.winslow.asblr.*;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.Requirements;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.config.UserInput;
import de.itd.tracking.winslow.fs.Event;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.PreparedStageBuilder;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.project.LogReader;
import de.itd.tracking.winslow.project.LogRepository;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
    @Nonnull private final String             nodeName;

    @Nonnull private final Map<String, Executor> executors         = new ConcurrentHashMap<>();
    @Nonnull private final DelayedExecutor       delayedExecutions = new DelayedExecutor();

    private boolean executeStages;


    public Orchestrator(
            @Nonnull LockBus lockBus,
            @Nonnull Environment environment,
            @Nonnull Backend backend,
            @Nonnull ProjectRepository projects,
            @Nonnull PipelineRepository pipelines,
            @Nonnull RunInfoRepository hints,
            @Nonnull LogRepository logs,
            @Nonnull String nodeName,
            boolean executeStages) {
        this.lockBus       = lockBus;
        this.environment   = environment;
        this.backend       = backend;
        this.projects      = projects;
        this.pipelines     = pipelines;
        this.hints         = hints;
        this.logs          = logs;
        this.nodeName      = nodeName;
        this.executeStages = executeStages;

        this.lockBus.registerEventListener(Event.Command.KILL, this::handleKillEvent);
        this.lockBus.registerEventListener(Event.Command.RELEASE, this::handleReleaseEvent);

        this.pollAllPipelinesForUpdate();
    }

    private void handleReleaseEvent(@Nonnull Event event) {
        var project = this.projects.getProjectIdForLockSubject(event.getSubject());
        if (project.isPresent()) {
            LOG.info("Going to check project for changes: " + project.get());
            this.delayedExecutions.executeRandomlyDelayed(project.get(), 10, 100, () -> {
                this.pollPipelineForUpdate(project.get());
            });
        }
    }

    private void handleKillEvent(@Nonnull Event event) {
        if (null != this.executors.remove(event.getSubject())) {
            try {
                this.backend.kill(event.getSubject());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    @Nonnull
    public Backend getBackend() {
        return backend;
    }

    private void pollPipelineForUpdate(@Nonnull String id) {
        this.checkPipelinesForUpdate(Stream.of(this.pipelines.getPipeline(id)));
    }

    private void pollAllPipelinesForUpdate() {
        this.checkPipelinesForUpdate(this.pipelines.getAllPipelines());
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
            var hasUpdate    = pipe.map(this::isStageStateUpdateAvailable).orElse(false);
            var inconsistent = pipe.map(this::needsConsistencyUpdate).orElse(false);
            var capable      = pipe.flatMap(this::isCapableOfExecutingNextStage).orElse(false);

            LOG.info("Checking, locked=" + locked + ", hasUpdate=" + hasUpdate + ", capable=" + capable + ", inconsistent=" + inconsistent + " projectId=" + projectId);

            return !locked && ((hasUpdate && capable) || inconsistent);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Failed to poll for " + handle.unsafe().map(Pipeline::getProjectId), t);
            return false;
        }
    }

    private Optional<Boolean> isCapableOfExecutingNextStage(@Nonnull Pipeline pipeline) {
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

        try {
            if (project.isPresent()) {
                updatePipeline(project.get().getPipelineDefinition(), container);
            } else {
                LOG.warning("Failed to load project for project " + projectId + ", " + container.getLock());
            }
        } catch (OrchestratorException e) {
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
        try (container; var heart = new LockHeart(container.getLock())) {
            var pipelineOpt = container.get();
            if (pipelineOpt.isPresent()) {
                var pipeline = tryUpdateContainer(container, updateRunningStage(pipelineOpt.get()));
                if (this.executeStages && pipeline
                        .getRunningStage()
                        .map(Stage::getState)
                        .orElse(null) != Stage.State.Running) {
                    tryStartNextPipelineStage(definition, container, pipeline);
                }
            }
        } catch (LockException e) {
            LOG.log(Level.SEVERE, "Failed to get pipeline for update", e);
        }
    }

    private void tryStartNextPipelineStage(
            @Nonnull PipelineDefinition definition,
            @Nonnull LockedContainer<Pipeline> container,
            @Nonnull Pipeline pipeline) throws OrchestratorException {
        try {
            if (startNextStageIfReady(definition, pipeline)) {
                tryUpdateContainer(container, pipeline);
            }
        } catch (UserInputChecker.MissingUserInputException e) {
            pipeline.requestPause(Pipeline.PauseReason.FurtherInputRequired);
            tryUpdateContainer(container, pipeline);
        } catch (UserInputChecker.MissingUserConfirmationException e) {
            pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
            tryUpdateContainer(container, pipeline);
        } catch (AssemblyException e) {
            throw new OrchestratorException("Failed to assemble stage for execution", e);
        }
    }

    private boolean startNextStageIfReady(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) throws AssemblyException {
        var noneRunning = pipeline.getRunningStage().isEmpty();
        var paused      = pipeline.isPauseRequested();
        var hasNext     = pipeline.peekNextStage().isPresent();
        var isCapable   = isCapableOfExecutingNextStage(pipeline).orElse(false);

        if (noneRunning && !paused && hasNext && isCapable) {
            switch (pipeline.getStrategy()) {
                case MoveForwardOnce:
                    pipeline.requestPause();
                case MoveForwardUntilEnd:
                    if (startNextPipelineStage(definition, pipeline)) {
                        pipeline.clearPauseReason();
                        return true;
                    }
            }
        }
        return false;
    }

    private boolean startNextPipelineStage(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) throws AssemblyException {
        var nextStage = pipeline.peekNextStage();

        if (nextStage.isEmpty()) {
            LOG.warning("Got commanded to start next stage but there is none!");
            return false;
        }

        var stageEnqueued = nextStage.get();
        var projectId     = pipeline.getProjectId();
        var stageId       = getStageId(pipeline, stageEnqueued.getDefinition());
        var assembler     = new StageAssembler();
        var executor      = (Executor) null;

        try {
            executor = startExecutor(
                    pipeline.getProjectId(),
                    stageId,
                    getProgressHintMatcher(pipeline.getProjectId())
            );

            assembler
                    .add(new UserInputChecker())
                    .add(new DockerImageAppender())
                    .add(new RequirementAppender())
                    .add(new InternalEnvironmentVariableAppender())
                    .add(new WorkspaceCreator(environment.getResourceManager()))
                    .add(new NfsWorkspaceMount((NfsWorkDirectory) environment.getWorkDirectoryConfiguration()))
                    .add(new BuildAndSubmit(this.nodeName))
                    .assemble(new Context(
                            pipeline,
                            definition,
                            executor,
                            stageEnqueued,
                            stageId,
                            backend.newStageBuilder(projectId, stageId, stageEnqueued.getDefinition())
                    ));
            return true;
        } catch (AssemblyException | LockException | FileNotFoundException e) {
            LOG.log(Level.SEVERE, "Failed to start next stage of pipeline " + pipeline.getProjectId(), e);
            if (executor != null) {
                executor.logErr("Assembly failed");
                executor.stop();
                var stage = new Stage(
                        stageId,
                        stageEnqueued.getDefinition(),
                        stageEnqueued.getAction(),
                        null
                );
                stage.finishNow(Stage.State.Failed);

                pipeline.popNextStage();
                pipeline.pushStage(stage);
                pipeline.pushStage(null);
            }
            try {
                this.backend.delete(pipeline.getProjectId(), stageId);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Force purging on the Backend failed", ex);
            }
            return true;
        }
    }

    @Nonnull
    private Pipeline updateRunningStage(@Nonnull Pipeline pipeline) {
        pipeline.getRunningStage().ifPresent(stage -> {
            LOG.info("Checking if running stage state can be updated: " + getStateOmitExceptions(
                    pipeline,
                    stage
            ) + " for " + stage.getId());
            Supplier<Stage.State> finishStateOrFailed = () -> stage.getFinishState().orElse(Stage.State.Failed);
            switch (getStateOmitExceptions(pipeline, stage).orElseGet(finishStateOrFailed)) {
                case Running:
                    if (getLogRedirectionState(pipeline) != SimpleState.Failed) {
                        break;
                    }
                default:
                case Failed:
                    stage.finishNow(Stage.State.Failed);
                    pipeline.pushStage(null);
                    pipeline.requestPause(Pipeline.PauseReason.StageFailure);
                    try {
                        backend.kill(stage.getId());
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to request kill failed stage: " + stage.getId(), e);
                    }
                    break;
                case Succeeded:
                    stage.finishNow(Stage.State.Succeeded);
                    pipeline.pushStage(null);
                    break;
            }
        });
        return pipeline;
    }

    @Nonnull
    private Optional<Stage.State> getStateOmitExceptions(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        try {
            return getState(pipeline, stage);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to retrieve stage state", e);
            return Optional.empty();
        }
    }

    @Nonnull
    private Optional<Stage.State> getState(
            @Nonnull Pipeline pipeline,
            @Nonnull Stage stage) throws IOException {
        // faster & cheaper than potentially causing a REST request on a new TcpConnection
        if (this.executors.get(stage.getId()) != null) {
            return Optional.of(Stage.State.Running);
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

            if (hints.hasLogRedirectionCompletedSuccessfullyHint(projectId, stageId)) {
                return SimpleState.Succeeded;
            } else if (!logs.isLocked(projectId, stageId)) {
                LOG.warning("Detected log redirect which has been aborted! " + stageId + "@" + projectId);
                return SimpleState.Failed;
            } else {
                return SimpleState.Running;
            }
        }
    }

    private Consumer<LogEntry> getProgressHintMatcher(@Nonnull String projectId) {
        return entry -> {
            var matcher = PROGRESS_HINT_PATTERN.matcher(entry.getMessage());
            if (matcher.find()) {
                this.hints.setProgressHint(projectId, Math.round(Float.parseFloat(matcher.group(1))));
                LOG.finest(() -> "ProgressHint match: " + matcher.group(1));
            }
        };
    }

    private boolean isStageStateUpdateAvailable(Pipeline pipeline) {
        return pipeline
                .getRunningStage()
                .flatMap(stage -> getStateOmitExceptions(pipeline, stage))
                .map(state -> {
                    switch (state) {
                        default:
                        case Running:
                            return false;
                        case Succeeded:
                        case Failed:
                            return true;
                    }
                })
                .orElseGet(() -> this.executeStages
                        && pipeline.hasEnqueuedStages()
                        && !pipeline.isPauseRequested()
                        && getLogRedirectionState(pipeline) != SimpleState.Running
                );
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
            workspace.ifPresent(Orchestrator::forcePurgeWorkspace);
            return workspace.isPresent() && container.deleteOmitExceptions();

        } catch (LockException e) {
            throw new OrchestratorException("Failed to maintain lock", e);
        }
    }

    private static void forcePurgeWorkspace(@Nonnull Path workspace) {
        try {
            var maxRetries = 3;
            for (int i = 0; i < maxRetries && workspace.toFile().exists(); ++i) {
                var index = i;
                try (var stream = Files.walk(workspace)) {
                    stream.forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry);
                        } catch (NoSuchFileException ignored) {
                        } catch (IOException e) {
                            if (index + 1 == maxRetries) {
                                LOG.log(Level.WARNING, "Failed to delete: " + entry, e);
                            }
                        }
                    });
                }
            }
            Files.deleteIfExists(workspace);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to get rid of workspace directory " + workspace, e);
        }
    }

    @Nonnull
    public Pipeline createPipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.pipelines.getPipeline(project.getId()).unsafe().isPresent()) {
            throw new PipelineAlreadyExistsException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            Pipeline pipeline = new Pipeline(project.getId());
            tryCreateInitDirectory(pipeline);
            tryUpdateContainer(container, pipeline);
            tryStartNextPipelineStage(project.getPipelineDefinition(), container, pipeline);
            return pipeline;
        }
    }

    private void tryCreateInitDirectory(@Nonnull Pipeline pipeline) throws OrchestratorException {
        environment
                .getResourceManager()
                .createWorkspace(createWorkspaceInitPath(pipeline.getProjectId()), true)
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

    @Nonnull
    private static PreparedStageBuilder addGpuRequirement(PreparedStageBuilder builder, Requirements.Gpu gpu) {
        builder = builder.withGpuCount(gpu.getCount());

        if (gpu.getVendor().isPresent()) {
            builder = builder.withGpuVendor(gpu.getVendor().get());
        }
        return builder;
    }

    private static boolean isConfirmed(@Nonnull Pipeline pipeline) {
        return Pipeline.ResumeNotification.Confirmation == pipeline.getResumeNotification().orElse(null);
    }

    private static Stream<String> hasMissingUserInput(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull PreparedStageBuilder builder) {
        return Stream.concat(
                pipelineDefinition.getRequires().stream().flatMap(u -> u.getEnvironment().stream()),
                stageDefinition.getRequires().stream().flatMap(u -> u.getEnvironment().stream())
        ).filter(k -> builder.getEnvVariable(k).isEmpty());
    }

    private static boolean isConfirmationRequiredForNextStage(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Pipeline pipeline) {
        return Stream
                .concat(stageDefinition.getRequires().stream(), pipelineDefinition.getRequires().stream())
                .filter(u -> u.getConfirmation() != UserInput.Confirmation.Never)
                .anyMatch(u -> !(u.getConfirmation() == UserInput.Confirmation.Once && pipeline
                        .getAllStages()
                        .anyMatch(s -> s.getDefinition().equals(stageDefinition))));
    }

    private static Path createWorkspacePathFor(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return createWorkspacePathFor(pipeline.getProjectId(), pipeline.getStageCount() + 1, stage.getName());
    }

    private static Path createWorkspaceInitPath(@Nonnull String projectId) {
        return createWorkspacePathFor(projectId, 0, null);
    }

    private static Path createWorkspacePathFor(@Nonnull String projectId, int stageNumber, @Nullable String suffix) {
        return Path.of(
                projectId,
                replaceInvalidCharactersInJobName(String.format(
                        "%04d%s%s",
                        stageNumber,
                        suffix != null ? "_" : "",
                        suffix != null ? suffix : ""
                ))
        );
    }

    private static Path getWorkspacePathForPipeline(@Nonnull Pipeline pipeline) {
        return Path.of(pipeline.getProjectId());
    }

    private static String getStageId(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return replaceInvalidCharactersInJobName(String.format(
                "%s_%04d_%s",
                pipeline.getProjectId(),
                pipeline.getStageCount() + 1,
                stage.getName()
        ));
    }

    @Nonnull
    public Optional<Pipeline> getPipeline(@Nonnull Project project) {
        return pipelines.getPipeline(project.getId()).unsafe();
    }

    @Nonnull
    public <T> Optional<T> updatePipeline(
            @Nonnull Project project,
            @Nonnull Function<Pipeline, T> updater) {
        return pipelines.getPipeline(project.getId()).exclusive().flatMap(container -> {
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

    @Nonnull
    public Optional<Integer> getProgressHint(@Nonnull Project project) {
        return hints.getProgressHint(project.getId());
    }

    private Executor startExecutor(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull Consumer<LogEntry> consumer) throws LockException, FileNotFoundException {
        var executor = new Executor(pipeline, stage, this);
        executor.addShutdownListener(() -> this.executors.remove(stage));
        executor.addShutdownListener(() -> this.pollPipelineForUpdate(pipeline));
        executor.addLogEntryConsumer(consumer);
        this.executors.put(stage, executor);
        return executor;
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
