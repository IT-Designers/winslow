package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.LockedOutputStream;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.project.LogReader;
import de.itd.tracking.winslow.project.LogRepository;
import de.itd.tracking.winslow.project.LogWriter;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NomadOrchestrator implements Orchestrator {

    private static final Logger  LOG                     = Logger.getLogger(NomadOrchestrator.class.getSimpleName());
    private static final Pattern INVALID_NOMAD_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-_]");
    private static final Pattern MULTI_UNDERSCORE        = Pattern.compile("_[_]+");

    @Nonnull private final Environment     environment;
    @Nonnull private final NomadApiClient  client;
    @Nonnull private final NomadRepository repository;
    @Nonnull private final LogRepository   logs;

    private boolean isRunning = false;
    private boolean shouldRun = true;

    public NomadOrchestrator(@Nonnull Environment environment, @Nonnull NomadApiClient client, @Nonnull NomadRepository repository, @Nonnull LogRepository logs) {
        this.environment = environment;
        this.client      = client;
        this.repository  = repository;
        this.logs        = logs;

        var thread = new Thread(this::pipelineUpdaterLoop);
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName());
        thread.start();
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
                pollAllPipelinesForUpdate();
                try {
                    // TODO
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            this.isRunning = false;
        }
    }

    private void pollAllPipelinesForUpdate() {
        repository.getAllPipelines().filter(handle -> {
            try {
                return handle.unsafe().map(this::hasUpdateAvailable).orElse(false);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to poll for " + handle.unsafe().map(NomadPipeline::getProjectId), t);
                return false;
            }
        }).map(BaseRepository.Handle::exclusive).flatMap(Optional::stream).forEach(container -> {
            try {
                updatePipeline(container);
            } catch (OrchestratorException e) {
                LOG.log(Level.SEVERE, "Failed to update pipeline " + container
                        .getNoThrow()
                        .map(NomadPipeline::getProjectId), e);
            }
        });
    }

    private void updatePipeline(LockedContainer<NomadPipeline> container) throws OrchestratorException {
        try (container; var heart = new LockHeart(container.getLock())) {
            var pipelineOpt = container.get();
            if (pipelineOpt.isPresent()) {
                var pipeline = tryUpdateContainer(container, updateRunningStage(pipelineOpt.get()));
                var stage    = maybeStartNextStage(pipeline);

                if (stage.isPresent()) {
                    tryUpdateContainer(container, pipeline, stage.get());
                }
            }
        } catch (LockException e) {
            LOG.log(Level.SEVERE, "Failed to get pipeline for update", e);
        }
    }

    private void handleIncompleteStageException(NomadPipeline pipeline, IncompleteStageException e) {
        LOG.log(Level.SEVERE, "Failed to start new stage", e);
        e.getStage().ifPresent(s -> this.forcePurgeJob(pipeline, s));
        e.getWorkspace().ifPresent(NomadOrchestrator::forcePurgeWorkspace);
    }

    private void forcePurgeStage(@Nonnull NomadPipeline pipeline, @Nonnull NomadStage stage) {
        forcePurgeJob(pipeline, stage);
        forcePurgeWorkspace(pipeline, stage);
    }

    private void forcePurgeWorkspace(@Nonnull NomadPipeline pipeline, @Nonnull NomadStage stage) {
        environment
                .getResourceManager()
                .getWorkspace(getWorkspacePathForStage(pipeline, stage.getTaskName()))
                .ifPresent(NomadOrchestrator::forcePurgeWorkspace);
    }

    private static void forcePurgeWorkspace(@Nonnull Path workspace) {
        try {
            for (int i = 0; i < 3; ++i) {
                try (var stream = Files.walk(workspace)) {
                    stream.forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry);
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

    private void forcePurgeJob(@Nonnull NomadPipeline pipeline, @Nonnull NomadStage stage) {
        try {
            this.client.getJobsApi().deregister(stage.getJobId());
        } catch (IOException | NomadException e) {
            LOG.log(Level.SEVERE, "Failed to deregister job " + pipeline.getProjectId() + "." + stage.getJobId() + "/" + stage
                    .getTaskName(), e);
        }
    }

    @Nonnull
    private Optional<NomadStage> maybeStartNextStage(NomadPipeline pipeline) throws OrchestratorException {
        if (pipeline.getRunningStage().isEmpty() && !pipeline.isPauseRequested() && pipeline
                .getNextStage()
                .isPresent()) {
            switch (pipeline.getStrategy()) {
                case MoveForwardOnce:
                    pipeline.requestPause();
                case MoveForwardUntilEnd:
                    var stage = tryStartNextPipelineStage(pipeline);
                    pipeline.pushStage(stage);
                    return Optional.of(stage);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    private NomadPipeline updateRunningStage(@Nonnull NomadPipeline pipeline) {
        pipeline.getRunningStage().ifPresent(stage -> {
            getStateOmitExceptions(stage).ifPresent(state -> {
                switch (state) {
                    default:
                    case Running:
                        break;
                    case Failed:
                        pipeline.requestPause();
                    case Succeeded:
                        stage.finishNow(state);
                        pipeline.pushStage(null);
                        break;
                }
            });
        });
        return pipeline;
    }

    @Nonnull
    private Optional<Stage.State> getState(NomadStage stage) throws IOException, NomadException {
        return this
                .getJobAllocationContainingTaskState(stage.getJobId(), stage.getTaskName())
                .flatMap(alloc -> NomadOrchestrator.toRunningStageState(alloc, stage.getTaskName()));
    }

    @Nonnull
    private Optional<Stage.State> getStateOmitExceptions(NomadStage stage) {
        try {
            return getState(stage);
        } catch (NomadException | IOException e) {
            LOG.log(Level.SEVERE, "Failed to retrieve stage state", e);
            return Optional.empty();
        }
    }

    private boolean hasUpdateAvailable(NomadPipeline pipeline) {
        return isStageStateUpdateAvailable(pipeline);
    }

    private boolean isStageStateUpdateAvailable(NomadPipeline pipeline) {
        return pipeline.getRunningStage().flatMap(this::getStateOmitExceptions).map(state -> {
            switch (state) {
                default:
                case Running:
                    return false;
                case Succeeded:
                case Failed:
                    return true;
            }
        }).orElseGet(() -> pipeline.getNextStage().isPresent() && !pipeline.isPauseRequested());
    }

    private static String combine(String... names) {
        return String.join("-", names);
    }

    @Nonnull
    @Override
    public Pipeline createPipeline(@Nonnull Project project) throws OrchestratorException {
        if (this.repository.getNomadPipeline(project.getId()).unsafe().isPresent()) {
            throw new PipelineAlreadyExistsException(project);
        }

        try (var container = exclusivePipelineContainer(project); var heart = new LockHeart(container.getLock())) {
            NomadPipeline pipeline = new NomadPipeline(project.getId(), project.getPipelineDefinition());
            NomadStage    stage    = tryStartNextPipelineStage(pipeline);

            pipeline.pushStage(stage);
            return tryUpdateContainer(container, pipeline, stage);
        }
    }

    private NomadPipeline tryUpdateContainer(LockedContainer<NomadPipeline> container, NomadPipeline pipeline) throws OrchestratorException {
        try {
            container.update(pipeline);
            return pipeline;
        } catch (IOException e) {
            throw new OrchestratorException("Failed to update pipeline", e);
        }
    }

    private NomadPipeline tryUpdateContainer(LockedContainer<NomadPipeline> container, NomadPipeline pipeline, NomadStage stage) throws OrchestratorException {
        try {
            container.update(pipeline);
            return pipeline;
        } catch (IOException e) {
            this.forcePurgeStage(pipeline, stage);
            throw new OrchestratorException("Failed to update pipeline", e);
        }
    }

    private NomadStage tryStartNextPipelineStage(NomadPipeline pipeline) throws OrchestratorException {
        try {
            return this.startNextPipelineStage(pipeline);
        } catch (IncompleteStageException e) {
            handleIncompleteStageException(pipeline, e);
            throw new OrchestratorException("Failed to start new stage", e.getCause());
        }
    }

    private LockedContainer<NomadPipeline> exclusivePipelineContainer(@Nonnull Project project) throws OrchestratorException {
        return this.repository
                .getNomadPipeline(project.getId())
                .exclusive()
                .orElseThrow(() -> new OrchestratorException("Failed to access new pipeline exclusively"));
    }

    private NomadStage startNextPipelineStage(NomadPipeline pipeline) throws IncompleteStageException {
        var stageDefinition = pipeline
                .getNextStage()
                .orElseThrow(() -> new IncompleteStageException(null, null, "A pipeline requires at least one stage"));

        var taskName      = getTaskName(pipeline, stageDefinition);
        var workspacePath = getWorkspacePathForStage(pipeline, taskName);
        var builder       = JobBuilder.withRandomUuid().withTaskName(taskName);

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(workspacePath, true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new IncompleteStageException(null, environment
                    .getResourceManager()
                    .getWorkspace(workspacePath)
                    .orElse(null), "The workspace and resources directory must exit, but at least one isn't. workspace=" + workspace + ",resources=" + resources);
        }

        copyContentOfMostRecentStageTo(pipeline, workspace.get());

        if (stageDefinition.getImage().isPresent()) {
            builder = builder
                    .withDockerImage(stageDefinition.getImage().get().getName())
                    .withDockerImageArguments(stageDefinition.getImage().get().getArguments());
        }

        if (environment.getWorkDirectoryConfiguration() instanceof NfsWorkDirectory) {
            var config = (NfsWorkDirectory) environment.getWorkDirectoryConfiguration();

            var exportedResources = resources.flatMap(config::toExportedPath);
            var exportedWorkspace = workspace.flatMap(config::toExportedPath);

            if (exportedResources.isEmpty() || exportedWorkspace.isEmpty()) {
                workspace.map(Path::toFile).map(File::delete);
                throw new IncompleteStageException(null, workspace.get(), "The workspace and resource path must be exported, but at least one isn't. workspace=" + exportedWorkspace + ",resources=" + exportedResources);
            }

            System.out.println(resources);
            System.out.println(workspace);
            System.out.println(exportedResources);
            System.out.println(exportedWorkspace);

            builder = builder
                    .addNfsVolume("winslow-" + builder.getId() + "-resources", "/resources", true, config.getOptions(), exportedResources
                            .get()
                            .toAbsolutePath()
                            .toString())
                    .addNfsVolume("winslow-" + builder.getId() + "-workspace", "/workspace", false, config.getOptions(), exportedWorkspace
                            .get()
                            .toAbsolutePath()
                            .toString());
        } else {
            throw new IncompleteStageException(null, workspace.get(), "Unknown WorkDirectoryConfiguration: " + environment
                    .getWorkDirectoryConfiguration());
        }


        NomadStage stage;

        try {
            var job = builder.buildJob(pipeline.getDefinition(), stageDefinition, environment);
            stage = new PreparedJob(job, this).start(stageDefinition);
        } catch (OrchestratorException e) {
            throw new IncompleteStageException(null, workspace.get(), "Failed to start stage", e);
        }

        pipeline.incrementNextStageIndex(); // at this point, the start was successful
        redirectLogs(pipeline, stage);
        return stage;
    }

    private static Path getWorkspacePathForStage(NomadPipeline pipeline, String taskName) {
        return Path.of(pipeline.getProjectId(), taskName);
    }

    private void copyContentOfMostRecentStageTo(NomadPipeline pipeline, Path workspace) throws IncompleteStageException {
        var workDirBefore = pipeline
                .getMostRecentStage()
                .flatMap(stageBefore -> environment
                        .getResourceManager()
                        .getWorkspace(getWorkspacePathForStage(pipeline, stageBefore.getTaskName())));

        if (workDirBefore.isPresent()) {
            var dirBefore = workDirBefore.get();
            var failure   = Optional.<IOException>empty();

            try {
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
                failure = Optional.of(e);
            }

            if (failure.isPresent()) {
                throw new IncompleteStageException(null, workspace, "Failed to prepare workspace", failure.get());
            }
        }
    }

    private static String getTaskName(NomadPipeline pipeline, StageDefinition stage) {
        return String.format("%04d_%s", pipeline.getStageCount(), stage.getName());
    }


    @Nonnull
    @Override
    public Optional<NomadPipeline> getPipeline(@Nonnull Project project) {
        return repository.getNomadPipeline(project.getId()).unsafe();
    }

    @Nonnull
    @Override
    public <T> Optional<T> updatePipeline(@Nonnull Project project, @Nonnull Function<Pipeline, T> updater) throws OrchestratorException {
        return repository.getNomadPipeline(project.getId()).exclusive().flatMap(container -> {
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
    @Override
    public Optional<NomadPipeline> getPipelineForStageId(@Nonnull String stageId) {
        return repository
                .getAllPipelines()
                .flatMap(pipeline -> pipeline.unsafe().stream())
                .filter(pipeline -> pipeline.getAllStages().anyMatch(stage -> stage.getJobId().equals(stageId)))
                .findFirst();
    }

    @Nonnull
    @Override
    public Optional<String> getProjectIdForPipeline(@Nonnull Pipeline pipeline) {
        if (pipeline instanceof NomadPipeline) {
            return Optional.of(((NomadPipeline) pipeline).getProjectId());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull String stageId) {
        try {
            return LogReader.stream(logs.getRawInputStreamNonExclusive(project.getId(), stageId));
        } catch (FileNotFoundException e) {
            return Stream.empty();
        }
    }


    @Nonnull
    NomadApiClient getClient() {
        return new NomadApiClient(this.client.getConfig());
    }

    @Nonnull
    private ClientApi getClientApi() {
        return getClient().getClientApi(this.client.getConfig().getAddress());
    }

    private void redirectLogs(@Nonnull NomadPipeline pipeline, @Nonnull NomadStage stage) {
        new Thread(() -> {
            try (LockedOutputStream os = logs.getRawOutputStream(pipeline.getProjectId(), stage.getId())) {
                var stdout = LogStream.stdOut(getClientApi(), stage.getTaskName(), () -> getJobAllocationContainingTaskStateLogErrors(stage
                        .getJobId(), stage.getTaskName()));
                var stderr = LogStream.stdErr(getClientApi(), stage.getTaskName(), () -> getJobAllocationContainingTaskStateLogErrors(stage
                        .getJobId(), stage.getTaskName()));

                var queue = new LinkedList<LogEntry>();


                var threadStdOut = new Thread(() -> {
                    stdout.forEach(bytes -> {
                        synchronized (queue) {
                            queue.add(bytes);
                        }
                    });
                });
                threadStdOut.setName(stage.getJobId() + ".stdout");
                threadStdOut.setDaemon(true);
                threadStdOut.start();

                var threadStdErr = new Thread(() -> {
                    stderr.forEach(bytes -> {
                        synchronized (queue) {
                            queue.add(bytes);
                        }
                    });
                });
                threadStdErr.setName(stage.getJobId() + ".stderr");
                threadStdErr.setDaemon(true);
                threadStdErr.start();

                LogWriter.foreground(os, prev -> {
                    synchronized (queue) {
                        return threadStdErr.isAlive() || threadStdOut.isAlive() || !queue.isEmpty();
                    }
                }, () -> {
                    synchronized (queue) {
                        return queue.poll();
                    }
                });

                os.flush();
            } catch (LockException | IOException e) {
                LOG.log(Level.SEVERE, "Log writer failed", e);
            }
        }).start();
    }


    @Nonnull
    private LogIterator getLogIteratorStdOut(NomadStage stage, int lastNLines) {
        return getLogIterator(stage, lastNLines, "stdout");
    }

    @Nonnull
    private LogIterator getLogIteratorStdErr(NomadStage stage, int lastNLines) {
        return getLogIterator(stage, lastNLines, "stderr");
    }

    @Nonnull
    private LogIterator getLogIterator(@Nonnull NomadStage stage, int lastNLines, String logType) {
        return new LogIterator(stage.getJobId(), stage.getTaskName(), logType, getClientApi(), () -> getJobAllocationContainingTaskStateLogErrors(stage
                .getJobId(), stage.getTaskName()));
    }

    @Nonnull
    private Optional<AllocationListStub> getJobAllocationContainingTaskStateLogErrors(@Nonnull String jobId, @Nonnull String taskName) {
        try {
            return getJobAllocationContainingTaskState(jobId, taskName);
        } catch (NomadException | IOException e) {
            LOG.log(Level.WARNING, "Failed to get job allocation");
            return Optional.empty();
        }
    }

    private Optional<AllocationListStub> getJobAllocationContainingTaskState(@Nonnull String jobId, @Nonnull String taskName) throws IOException, NomadException {
        for (AllocationListStub allocationListStub : getClient().getAllocationsApi().list().getValue()) {
            if (jobId.equals(allocationListStub.getJobId())) {
                if (allocationListStub.getTaskStates() != null && allocationListStub
                        .getTaskStates()
                        .get(taskName) != null) {
                    return Optional.of(allocationListStub);
                }
            }
        }
        return Optional.empty();
    }

    @Nonnull
    static Optional<Boolean> hasTaskStarted(AllocationListStub allocation, String taskName) {
        return Optional
                .ofNullable(allocation.getTaskStates().get(taskName))
                .map(state -> state.getStartedAt().after(new Date(1)));
    }

    @Nonnull
    public static Optional<Boolean> hasTaskFinished(AllocationListStub allocation, String taskName) {
        return Optional
                .ofNullable(allocation.getTaskStates().get(taskName))
                .map(state -> state.getFinishedAt().after(new Date(1)));
    }

    @Nonnull
    public static Optional<Boolean> hasTaskFailed(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates().get(taskName)).map(TaskState::getFailed);
    }

    @Nonnull
    public static Optional<Stage.State> toRunningStageState(@Nonnull AllocationListStub allocation, @Nonnull String taskName) {
        var failed   = hasTaskFailed(allocation, taskName);
        var started  = hasTaskStarted(allocation, taskName);
        var finished = hasTaskFinished(allocation, taskName);

        if (failed.isPresent() && failed.get()) {
            return Optional.of(Stage.State.Failed);
        } else if (started.isPresent() && !started.get()) {
            return Optional.of(Stage.State.Running);
        } else if (started.isPresent() && finished.isPresent() && !finished.get()) {
            return Optional.of(Stage.State.Running);
        } else if (finished.isPresent() && finished.get()) {
            return Optional.of(Stage.State.Succeeded);
        } else {
            return Optional.empty();
        }
    }


    private static String replaceInvalidCharactersInJobName(@Nonnull String jobName) {
        return MULTI_UNDERSCORE
                .matcher(INVALID_NOMAD_CHARACTER.matcher(jobName.toLowerCase()).replaceAll("_"))
                .replaceAll("_");
    }
}
