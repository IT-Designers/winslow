package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.Requirements;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.config.UserInput;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.project.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Orchestrator {

    private static final Logger  LOG                     = Logger.getLogger(Orchestrator.class.getSimpleName());
    private static final Pattern INVALID_NOMAD_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-_]");
    private static final Pattern MULTI_UNDERSCORE        = Pattern.compile("_[_]+");
    public static final  Pattern PROGRESS_HINT_PATTERN   = Pattern.compile("(([\\d]+[.])?[\\d]+)[ ]*%");

    @Nonnull private final Environment        environment;
    @Nonnull private final Backend            backend;
    @Nonnull private final ProjectRepository  projects;
    @Nonnull private final PipelineRepository pipelines;
    @Nonnull private final RunInfoRepository  hints;
    @Nonnull private final LogRepository      logs;
    @Nonnull private final String             nodeName;

    @Nonnull private final Map<String, Executor> executors = new ConcurrentHashMap<>();

    private boolean isRunning = false;
    private boolean shouldRun = false;


    public Orchestrator(
            @Nonnull Environment environment,
            @Nonnull Backend backend,
            @Nonnull ProjectRepository projects,
            @Nonnull PipelineRepository pipelines,
            @Nonnull RunInfoRepository hints,
            @Nonnull LogRepository logs,
            @Nonnull String nodeName) {
        this.environment = environment;
        this.backend     = backend;
        this.projects    = projects;
        this.pipelines   = pipelines;
        this.hints       = hints;
        this.logs        = logs;
        this.nodeName    = nodeName;
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

    public synchronized void start() {
        if (!this.shouldRun) {
            this.shouldRun = true;
            var thread = new Thread(this::pipelineUpdaterLoop);
            thread.setDaemon(true);
            thread.setName(getClass().getSimpleName());
            thread.start();
        }
    }

    public synchronized void stop() {
        this.shouldRun = false;
    }

    public synchronized boolean isGoingToStop() {
        return !this.shouldRun;
    }

    public synchronized boolean isRunning() {
        return this.isRunning;
    }

    private void pipelineUpdaterLoop() {
        this.isRunning = true;
        try {
            while (!isGoingToStop()) {
                try {
                    pollAllPipelinesForUpdate();
                    // TODO
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            this.isRunning = false;
        }
    }

    private void pollAllPipelinesForUpdate() {
        pipelines.getAllPipelines().filter(handle -> {
            try {
                return handle.unsafe().map(this::hasUpdateAvailable).orElse(false);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to poll for " + handle.unsafe().map(Pipeline::getProjectId), t);
                return false;
            }
        }).map(BaseRepository.Handle::exclusive).flatMap(Optional::stream).forEach(container -> {
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

        });
    }

    private void updatePipeline(
            @Nonnull PipelineDefinition definition,
            @Nonnull LockedContainer<Pipeline> container) throws OrchestratorException {
        try (container; var heart = new LockHeart(container.getLock())) {
            var pipelineOpt = container.get();
            if (pipelineOpt.isPresent()) {
                var pipeline = tryUpdateContainer(container, updateRunningStage(pipelineOpt.get()));
                tryStartNextPipelineStage(definition, container, pipeline);
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
            var stage = startNextStageIfReady(definition, pipeline);

            if (stage.isPresent()) {
                tryUpdateContainer(container, pipeline, stage.get());
            }
        } catch (IncompleteStageException e) {
            if (e.isConfirmationRequired()) {
                pipeline.requestPause(Pipeline.PauseReason.ConfirmationRequired);
                tryUpdateContainer(container, pipeline);
            }
            if (e.isMissingEnvVariables()) {
                pipeline.requestPause(Pipeline.PauseReason.FurtherInputRequired);
                tryUpdateContainer(container, pipeline);
            }
        }
    }

    private void cleanupIncompleteStage(Pipeline pipeline, IncompleteStageException e) {
        LOG.log(Level.SEVERE, "Failed to start new stage", e);
        e.getStage().ifPresent(s -> this.forcePurgeJob(pipeline, s));
        e.getWorkspace().ifPresent(Orchestrator::forcePurgeWorkspace);
    }

    private void forcePurgeStage(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        forcePurgeJob(pipeline, stage);
        forcePurgeWorkspace(pipeline, stage);
        forcePurgeExecutor(pipeline, stage);
    }

    private void forcePurgeExecutor(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        var executor = this.executors.remove(stage.getId());
        if (executor != null) {
            executor.logErr("Startup failed, force purge");
            executor.flush();
            executor.stop();
        }
    }

    private void forcePurgeWorkspace(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        environment
                .getResourceManager()
                .getWorkspace(getWorkspacePathForStage(pipeline, stage))
                .ifPresent(Orchestrator::forcePurgeWorkspace);
    }

    private static void forcePurgeWorkspace(@Nonnull Path workspace) {
        try {
            for (int i = 0; i < 3 && workspace.toFile().exists(); ++i) {
                try (var stream = Files.walk(workspace)) {
                    stream.forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry);
                        } catch (NoSuchFileException ignored) {
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to delete: " + entry, e);
                        }
                    });
                }
            }
            Files.deleteIfExists(workspace);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to get rid of worksapce directory " + workspace, e);
        }
    }

    private void forcePurgeJob(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        try {
            this.backend.delete(pipeline.getProjectId(), stage.getId());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to deregister job for " + pipeline.getProjectId() + "/" + stage.getId(), e);
        }
    }

    @Nonnull
    private Optional<Stage> startNextStageIfReady(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) throws OrchestratorException {
        if (pipeline.getRunningStage().isEmpty() && !pipeline.isPauseRequested() && pipeline
                .peekNextStage()
                .isPresent()) {
            switch (pipeline.getStrategy()) {
                case MoveForwardOnce:
                    pipeline.requestPause();
                case MoveForwardUntilEnd:
                    var stage = startNextPipelineStageAutoCleanupOnFailure(definition, pipeline);
                    pipeline.pushStage(stage);
                    pipeline.clearPauseReason();
                    return Optional.of(stage);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    private Pipeline updateRunningStage(@Nonnull Pipeline pipeline) {
        pipeline.getRunningStage().ifPresent(stage -> {
            LOG.info("Checking if running stage state can be updated: " + getStateOmitExceptions(pipeline, stage) + " for " + stage.getId());
            switch (getStateOmitExceptions(pipeline, stage).orElse(Stage.State.Failed)) {
                case Running:
                    if (getLogRedirectionState(pipeline) != SimpleState.Failed) {
                        break;
                    }
                default:
                case Failed:
                    stage.finishNow(Stage.State.Failed);
                    pipeline.pushStage(null);
                    pipeline.requestPause(Pipeline.PauseReason.StageFailure);
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

    private boolean hasUpdateAvailable(Pipeline pipeline) {
        // LOG.info(pipeline.getProjectId() + ".isStageStateUpdateAvailable=" + isStageStateUpdateAvailable(pipeline));
        // LOG.info(pipeline.getProjectId() + ".getLogRedirectionState=" + getLogRedirectionState(pipeline));
        return isStageStateUpdateAvailable(pipeline) || getLogRedirectionState(pipeline) == SimpleState.Failed;
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
                .orElseGet(() -> pipeline.peekNextStage().isPresent() && !pipeline.isPauseRequested());
    }

    @Nonnull
    public Pipeline createPipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.pipelines.getPipeline(project.getId()).unsafe().isPresent()) {
            throw new PipelineAlreadyExistsException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            Pipeline pipeline = new Pipeline(project.getId());
            tryUpdateContainer(container, pipeline);
            tryStartNextPipelineStage(project.getPipelineDefinition(), container, pipeline);
            return pipeline;
        }
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

    private Pipeline tryUpdateContainer(
            @Nonnull LockedContainer<Pipeline> container,
            @Nonnull Pipeline pipeline,
            @Nonnull Stage stage) throws OrchestratorException {
        try {
            container.update(pipeline);
            return pipeline;
        } catch (IOException e) {
            this.forcePurgeStage(pipeline, stage);
            throw new OrchestratorException("Failed to update pipeline", e);
        }
    }

    private Stage startNextPipelineStageAutoCleanupOnFailure(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) throws OrchestratorException {
        try {
            return this.startNextPipelineStage(definition, pipeline);
        } catch (IncompleteStageException e) {
            cleanupIncompleteStage(pipeline, e);
            throw e;
        }
    }

    private LockedContainer<Pipeline> exclusivePipelineContainer(@Nonnull Project project) throws OrchestratorException {
        return this.pipelines
                .getPipeline(project.getId())
                .exclusive()
                .orElseThrow(() -> new OrchestratorException("Failed to access new pipeline exclusively"));
    }

    private Stage startNextPipelineStage(
            @Nonnull PipelineDefinition definition,
            @Nonnull Pipeline pipeline) throws IncompleteStageException {
        var stageDefinition = pipeline.peekNextStage().orElseThrow(() -> IncompleteStageException.Builder
                .create("A pipeline requires at least one stage")
                .build());

        var stageId       = getStageId(pipeline, stageDefinition);
        var workspacePath = createWorkspacePathFor(pipeline, stageDefinition);
        var builder       = backend.newStageBuilder(pipeline.getProjectId(), stageId, stageDefinition);

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(workspacePath, true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw IncompleteStageException.Builder
                    .create("The workspace and resources directory must exit, but at least one isn't. workspacePath=" + workspacePath + ",workspace=" + workspace + ",resources=" + resources)
                    .withWorkspace(workspace
                                           .or(() -> environment.getResourceManager().getWorkspace(workspacePath))
                                           .orElse(null))
                    .build();
        }

        if (stageDefinition.getImage().isPresent()) {
            builder = builder
                    .withDockerImage(stageDefinition.getImage().get().getName())
                    .withDockerImageArguments(stageDefinition.getImage().get().getArgs());
        }

        if (environment.getWorkDirectoryConfiguration() instanceof NfsWorkDirectory) {
            var config = (NfsWorkDirectory) environment.getWorkDirectoryConfiguration();

            var exportedResources = resources.flatMap(config::toExportedPath);
            var exportedWorkspace = workspace.flatMap(config::toExportedPath);

            if (exportedResources.isEmpty() || exportedWorkspace.isEmpty()) {
                workspace.map(Path::toFile).map(File::delete);
                throw IncompleteStageException.Builder
                        .create("The workspace and resource path must be exported, but at least one isn't. workspace=" + exportedWorkspace + ",resources=" + exportedResources)
                        .withWorkspace(workspace.get())
                        .build();
            }

            var targetDirResources = "/resources";
            var targetDirWorkspace = "/workspace";

            builder = builder
                    .addNfsVolume(
                            "winslow-" + builder.getId() + "-resources",
                            targetDirResources,
                            true,
                            config.getOptions(),
                            exportedResources.get().toAbsolutePath().toString()
                    )
                    .addNfsVolume(
                            "winslow-" + builder.getId() + "-workspace",
                            targetDirWorkspace,
                            false,
                            config.getOptions(),
                            exportedWorkspace.get().toAbsolutePath().toString()
                    )
                    .withInternalEnvVariable(Env.SELF_PREFIX + "_DIR_RESOURCES", targetDirResources)
                    .withInternalEnvVariable(Env.SELF_PREFIX + "_DIR_WORKSPACE", targetDirWorkspace)
                    .withWorkspaceWithinPipeline(workspace.get().getFileName().toString());
        } else {
            throw IncompleteStageException.Builder
                    .create("Unknown WorkDirectoryConfiguration: " + environment.getWorkDirectoryConfiguration())
                    .withWorkspace(workspace.get())
                    .build();
        }

        if (stageDefinition.getRequirements().isPresent()) {
            var requirements = stageDefinition.getRequirements().get();

            if (requirements.getGpu().isPresent()) {
                builder = addGpuRequirement(builder, requirements.getGpu().get());
            }
        }

        var timeMs = System.currentTimeMillis();
        var timeS  = timeMs / 1_000;
        builder = builder
                .withEnvVariables(stageDefinition.getEnvironment())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_PROJECT_ID", pipeline.getProjectId())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_PIPELINE_ID", pipeline.getProjectId())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_PIPELINE_NAME", definition.getName())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_ID", builder.getId())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_NAME", stageDefinition.getName())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_NUMBER", Integer.toString(pipeline.getStageCount()))
                .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_DATE_TIME", new Date(timeS).toString())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_EPOCH_TIME", Long.toString(timeS))
                .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_EPOCH_TIME_MS", Long.toString(timeMs));


        boolean requiresConfirmation = isConfirmationRequiredForNextStage(definition, stageDefinition, pipeline);
        boolean hasMissingUserInput  = hasMissingUserInput(definition, stageDefinition, builder);

        if (requiresConfirmation && isConfirmed(pipeline)) {
            requiresConfirmation = false;
        }

        if (requiresConfirmation || hasMissingUserInput) {
            throw IncompleteStageException.Builder
                    .create("Stage requires further user input")
                    .withWorkspace(workspace.get())
                    .maybeMissingEnvVariables(hasMissingUserInput)
                    .maybeRequiresConfirmation(requiresConfirmation)
                    .build();
        }


        try {
            copyContentOfMostRecentStageTo(pipeline, workspace.get());
            var stage = builder.build().start();
            pipeline.resetResumeNotification();
            pipeline.popNextStage();
            // redirectLogs(pipeline, stage, getProgressHintMatcher(pipeline.getProjectId()));

            var executor = startExecutor(
                    pipeline.getProjectId(),
                    stage.getId(),
                    getProgressHintMatcher(pipeline.getProjectId())
            );
            executor.logInf("Stage execution started on " + this.nodeName);

            return stage;
        } catch (OrchestratorException | LockException | FileNotFoundException e) {
            throw IncompleteStageException.Builder
                    .create("Failed to start stage")
                    .withCause(e)
                    .withWorkspace(workspace.get())
                    .build();
        }
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

    private static boolean hasMissingUserInput(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull PreparedStageBuilder builder) {
        return Stream.concat(
                pipelineDefinition.getUserInput().stream().flatMap(u -> u.getValueFor().stream()),
                stageDefinition.getUserInput().stream().flatMap(u -> u.getValueFor().stream())
        ).anyMatch(k -> builder.getEnvVariable(k).isEmpty());
    }

    private static boolean isConfirmationRequiredForNextStage(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Pipeline pipeline) {
        return Stream
                .concat(stageDefinition.getUserInput().stream(), pipelineDefinition.getUserInput().stream())
                .filter(u -> u.requiresConfirmation() != UserInput.Confirmation.Never)
                .anyMatch(u -> !(u.requiresConfirmation() == UserInput.Confirmation.Once && pipeline
                        .getAllStages()
                        .anyMatch(s -> s.getDefinition().equals(stageDefinition))));
    }

    private static Path createWorkspacePathFor(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return Path.of(
                pipeline.getProjectId(),
                replaceInvalidCharactersInJobName(String.format("%04d_%s", pipeline.getStageCount(), stage.getName()))
        );
    }

    private static Path getWorkspacePathForStage(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        return Path.of(pipeline.getProjectId(), stage.getWorkspace());
    }

    private void copyContentOfMostRecentStageTo(
            Pipeline pipeline,
            Path workspace) throws IncompleteStageException {
        var workDirBefore = pipeline
                .getAllStages()
                .filter(stage -> stage.getState() == Stage.State.Succeeded)
                .reduce((first, second) -> second) // get the last successful stage
                .flatMap(stageBefore -> environment
                        .getResourceManager()
                        .getWorkspace(getWorkspacePathForStage(pipeline, stageBefore)));

        if (workDirBefore.isPresent()) {
            var dirBefore = workDirBefore.get();
            var failure   = Optional.<IOException>empty();

            try {
                LOG.fine("Source workspace directory: " + workDirBefore.get());
                failure = Files.walk(workDirBefore.get()).flatMap(path -> {
                    try {
                        var file = path.toFile();
                        var dst  = workspace.resolve(dirBefore.relativize(path));
                        if (file.isDirectory()) {
                            Files.createDirectories(dst);
                        } else {
                            Files.copy(path, dst);
                        }
                        return Stream.empty();
                    } catch (IOException e) {
                        return Stream.of(e);
                    }
                }).findFirst();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to source workspace from " + workDirBefore.get() + ": " + e);
                failure = Optional.of(e);
            }

            if (failure.isPresent()) {
                throw IncompleteStageException.Builder
                        .create("Failed to prepare workspace")
                        .withWorkspace(workspace)
                        .withCause(failure.get())
                        .build();
            }
        } else {
            LOG.info("No previous valid workspace directory found for " + pipeline.getProjectId());
        }
    }

    private static String getStageId(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return replaceInvalidCharactersInJobName(String.format(
                "%s_%04d_%s",
                pipeline.getProjectId(),
                pipeline.getStageCount(),
                stage.getName()
        ));
    }

    @Nonnull
    public Optional<Pipeline> getPipeline(@Nonnull Project project) {
        return pipelines.getPipeline(project.getId()).unsafe();
    }

    @Nonnull
    public <T> Optional<T> updatePipelineOmitExceptions(
            @Nonnull Project project,
            @Nonnull Function<Pipeline, T> updater) {
        try {
            return updatePipeline(project, updater);
        } catch (OrchestratorException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Nonnull
    public <T> Optional<T> updatePipeline(
            @Nonnull Project project,
            @Nonnull Function<Pipeline, T> updater) throws OrchestratorException {
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
        var executor = new Executor(pipeline, stage, this, () -> this.executors.remove(stage));
        executor.addLogEntryConsumer(consumer);
        this.executors.put(stage, executor);
        return executor;
    }


    private static String replaceInvalidCharactersInJobName(@Nonnull String jobName) {
        return MULTI_UNDERSCORE
                .matcher(INVALID_NOMAD_CHARACTER.matcher(jobName.toLowerCase()).replaceAll("_"))
                .replaceAll("_");
    }

    private enum SimpleState {
        Running, Failed, Succeeded,
    }
}
