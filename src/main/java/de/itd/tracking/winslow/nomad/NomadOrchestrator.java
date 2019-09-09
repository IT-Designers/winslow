package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadOrchestrator implements Orchestrator {

    private static final Logger LOG = Logger.getLogger(NomadOrchestrator.class.getSimpleName());

    @Nonnull private final Environment     environment;
    @Nonnull private final NomadApiClient  client;
    @Nonnull private final NomadRepository repository;

    private boolean isRunning = false;
    private boolean shouldRun = true;

    public NomadOrchestrator(@Nonnull Environment environment, @Nonnull NomadApiClient client, @Nonnull NomadRepository repository) {
        this.environment = environment;
        this.client      = client;
        this.repository  = repository;

        var thread = new Thread(this::pipelineUpdaterLoop);
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName());
        thread.start();
    }

    private void pipelineUpdaterLoop() {
        this.isRunning = true;
        try {
            while (shouldRun) {
                pollForPipelineUpdates();
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

    private void pollForPipelineUpdates() {
        // TODO
    }

    private String combine(String... names) {
        return String.join("-", names);
    }

    @Nonnull
    @Override
    public Pipeline createPipeline(@Nonnull Project project, @Nonnull PipelineDefinition pipelineDefinition) throws OrchestratorException {
        if (this.repository.getNomadPipeline(project.getId()).unsafe().isPresent()) {
            throw new PipelineAlreadyExistsException(project);
        }

        try (var container = exclusivePipelineContainer(project)) {
            var pipeline = new NomadPipeline(this, project.getId(), pipelineDefinition);
            var stage    = this.setupPipeline(pipeline);

            pipeline.pushStage(stage);
            container.update(pipeline);

            return pipeline;
        } catch (IOException e) {
            throw new OrchestratorException("Failed to create pipeline for project " + project.getId(), e);
        }
    }

    private LockedContainer<NomadPipeline> exclusivePipelineContainer(@Nonnull Project project) throws OrchestratorException {
        return this.repository
                .getNomadPipeline(project.getId())
                .exclusive()
                .orElseThrow(() -> new OrchestratorException("Failed to access new pipeline exclusively"));
    }

    private NomadStage setupPipeline(NomadPipeline pipeline) throws OrchestratorException {
        var stageDefinition = pipeline
                .getNextStage()
                .orElseThrow(() -> new OrchestratorException("A pipeline requires at least one stage"));
        var builder = JobBuilder.withRandomUuid().withTaskName(getTaskName(pipeline, stageDefinition));

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(builder.getUuid(), true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new OrchestratorException("The workspace and resources directory must exit, but at least one isn't. workspace=" + workspace + ",resources=" + resources);
        }

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
                throw new OrchestratorException("The workspace and resource path must be exported, but at least one isn't. workspace=" + exportedWorkspace + ",resources=" + exportedResources);
            }

            System.out.println(resources);
            System.out.println(workspace);
            System.out.println(exportedResources);
            System.out.println(exportedWorkspace);

            builder = builder
                    .addNfsVolume("winslow-" + builder.getUuid() + "-resources", "/resources", true, config.getOptions(), exportedResources
                            .get()
                            .toAbsolutePath()
                            .toString())
                    .addNfsVolume("winslow-" + builder.getUuid() + "-workspace", "/workspace", false, config.getOptions(), exportedWorkspace
                            .get()
                            .toAbsolutePath()
                            .toString());
        } else {
            throw new OrchestratorException("Unknown WorkDirectoryConfiguration: " + environment.getWorkDirectoryConfiguration());
        }

        return new PreparedJob(builder.buildJob(pipeline.getDefinition(), stageDefinition, environment), this).start(stageDefinition);
    }

    private String getTaskName(NomadPipeline pipeline, StageDefinition stage) throws OrchestratorException {
        return replaceInvalidCharactersInJobName(combine(pipeline.getProjectId(), stage.getName()));
    }


    @Nonnull
    @Override
    public Optional<Pipeline> getPipeline(@Nonnull Project project) throws OrchestratorException {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public <T> Optional<T> updatePipeline(@Nonnull Project project, @Nonnull Function<Pipeline, T> updater) throws OrchestratorException {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<Stage> getSubmission(@Nonnull Project project) {
        return getCurrentAllocation(project).map(allocatedJob -> allocatedJob);
    }

    @Nonnull
    @Override
    public Optional<Stage> getSubmissionUnsafe(@Nonnull Project project) {
        return repository
                .getNomadPipeline(project.getId())
                .unsafe()
                .flatMap(nomadProject -> nomadProject.getCurrentAllocation(this));
    }

    @Override
    public boolean canProgress(@Nonnull Project project) {
        var job = getCurrentAllocation(project);
        return canProgress(project, job);
    }

    private boolean hasCompleted(@Nonnull AllocatedJob job) {
        try {
            return job.hasCompleted();
        } catch (OrchestratorConnectionException e) {
            LOG.log(Level.SEVERE, "Failed to check if job completed: " + job.getJobId(), e);
            return false;
        }
    }

    private boolean hasCompletedSuccessfully(@Nonnull AllocatedJob job) {
        try {
            return job.hasCompletedSuccessfully();
        } catch (OrchestratorConnectionException e) {
            LOG.log(Level.SEVERE, "Failed to check if job completed: " + job.getJobId(), e);
            return false;
        }
    }

    @Override
    public boolean canProgressLockFree(@Nonnull Project project) {
        var job = repository.getNomadPipeline(project.getId()).unsafe().flatMap(j -> j.getCurrentAllocation(this));
        return canProgress(project, job);
    }

    @Override
    public boolean hasPendingChanges(@Nonnull Project project) {
        var nomadProject = repository.getNomadPipeline(project.getId()).unsafe();
        var job          = nomadProject.flatMap(j -> j.getCurrentAllocation(this));
        return job.isPresent() && !job.get().getStateOmitExceptions().equals(nomadProject.get().getCurrentState());
    }

    @Override
    public void updateInternalState(@Nonnull Project project) {
        repository.getNomadPipeline(project.getId()).exclusive().ifPresent(lock -> {
            try (lock) {
                updateProjectState(project, lock);
            } catch (LockException e) {
                LOG.log(Level.SEVERE, "Failed to update project", e);
            }
        });
    }

    private void updateProjectState(@Nonnull Project project, LockedContainer<NomadProject> lock) throws LockException {
        var inner = lock.get();
        inner.flatMap(j -> j.getCurrentAllocation(this)).flatMap(Stage::getStateOmitExceptions).ifPresent(state -> {
            try {
                var p = inner.get();
                if (p.getCurrentState().map(s -> s != state).orElse(true)) {
                    p.updateCurrent(state);
                    lock.update(p);
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to update nomad project " + project.getId(), e);
            }
        });
    }

    private boolean canProgress(@Nonnull Project project, @Nonnull Optional<AllocatedJob> job) {
        return (job.isEmpty() || hasCompletedSuccessfully(job.get()) || (project.isForceProgressOnce() && hasCompleted(job
                .get()))) && project.getNextStageIndex() < project.getPipeline().getStageDefinitions().size();
    }

    @Nonnull
    public Optional<AllocatedJob> getCurrentAllocation(@Nonnull Project project) {
        return repository.getNomadPipeline(project.getId()).exclusive().flatMap(locked -> {
            try (locked) {
                return locked.get().flatMap(nomadProject -> nomadProject.getCurrentAllocation(this));
            } catch (LockException e) {
                LOG.log(Level.SEVERE, "Failed to lock nomad project file", e);
                return Optional.empty();
            }
        });
    }

    @Nonnull
    @Override
    public Optional<Stage> startNext(@Nonnull Project project, @Nonnull Environment environment) {
        var index  = project.getNextStageIndex();
        var stages = project.getPipeline().getStageDefinitions();

        if (index < 0 || index >= stages.size()) {
            return Optional.empty();
        }

        return repository.getNomadPipeline(project.getId()).exclusive().flatMap(locked -> {
            try (locked) {
                if (!canProgress(project, locked.get().flatMap(p -> p.getCurrentAllocation(this)))) {
                    return Optional.empty();
                }

                updateProjectState(project, locked);

                var stage    = stages.get(index);
                var prepared = prepare(project.getId(), project.getPipeline(), stage, environment);
                var running  = prepared.start();

                if (running.isPresent()) {
                    var jobId    = running.get().getJobId();
                    var taskName = running.get().getTaskName();
                    var nomad    = locked.get().orElseGet(NomadProject::new);

                    nomad.newCurrent(index, jobId, taskName);
                    locked.update(nomad);

                    project.setNextStageIndex(index + 1);
                    project.setForceProgressOnce(false);

                    return nomad.getCurrentAllocation(this);
                }
            } catch (OrchestratorException | IOException | LockException e) {
                LOG.log(Level.SEVERE, "Failed to start next stage for project " + project.getId(), e);
            }
            return Optional.empty();
        });
    }

    @Nonnull
    private PreparedJob prepare(String projectId, PipelineDefinition pipelineDefinition, StageDefinition stageDefinition, Environment environment) throws OrchestratorException {
        var builder = JobBuilder
                .withRandomUuid()
                .withTaskName(replaceInvalidCharactersInJobName(combine(projectId, pipelineDefinition.getName(), stageDefinition
                        .getName())));

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(builder.getUuid(), true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new OrchestratorException("The workspace and resources directory must exit, but at least one isn't. workspace=" + workspace + ",resources=" + resources);
        }

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
                throw new OrchestratorException("The workspace and resource path must be exported, but at least one isn't. workspace=" + exportedWorkspace + ",resources=" + exportedResources);
            }

            System.out.println(resources);
            System.out.println(workspace);
            System.out.println(exportedResources);
            System.out.println(exportedWorkspace);

            builder = builder
                    .addNfsVolume("winslow-" + builder.getUuid() + "-resources", "/resources", true, config.getOptions(), exportedResources
                            .get()
                            .toAbsolutePath()
                            .toString())
                    .addNfsVolume("winslow-" + builder.getUuid() + "-workspace", "/workspace", false, config.getOptions(), exportedWorkspace
                            .get()
                            .toAbsolutePath()
                            .toString());
        } else {
            throw new OrchestratorException("Unknown WorkDirectoryConfiguration: " + environment.getWorkDirectoryConfiguration());
        }

        return new PreparedJob(builder.buildJob(pipelineDefinition, stageDefinition, environment), this);
    }

    public NomadApiClient getClient() {
        return this.client;
    }

    public ClientApi getClientApi() {
        return this.client.getClientApi(this.client.getConfig().getAddress());
    }

    public Optional<AllocationListStub> getJobAllocationContainingTaskState(@Nonnull String jobId, @Nonnull String taskName) throws IOException, NomadException {
        for (AllocationListStub allocationListStub : client.getAllocationsApi().list().getValue()) {
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

    public static Optional<Boolean> hasTaskStarted(AllocationListStub allocation, String taskName) {
        return Optional
                .ofNullable(allocation.getTaskStates().get(taskName))
                .map(state -> state.getStartedAt().after(new Date(1)));
    }

    public static Optional<Boolean> hasTaskFinished(AllocationListStub allocation, String taskName) {
        return Optional
                .ofNullable(allocation.getTaskStates().get(taskName))
                .map(state -> state.getFinishedAt().after(new Date(1)));
    }

    public static Optional<Boolean> hasTaskFailed(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates().get(taskName)).map(TaskState::getFailed);
    }

    public static Optional<Stage.State> toRunningStageState(AllocationListStub allocation, String taskName) {
        var failed   = hasTaskFailed(allocation, taskName);
        var started  = hasTaskStarted(allocation, taskName);
        var finished = hasTaskFinished(allocation, taskName);

        if (failed.isPresent() && failed.get()) {
            return Optional.of(Stage.State.Failed);
        } else if (started.isPresent() && !started.get()) {
            return Optional.of(Stage.State.Preparing);
        } else if (started.isPresent() && finished.isPresent() && !finished.get()) {
            return Optional.of(Stage.State.Running);
        } else if (finished.isPresent() && finished.get()) {
            return Optional.of(Stage.State.Succeeded);
        } else {
            return Optional.empty();
        }
    }


    public static String replaceInvalidCharactersInJobName(String jobName) {
        return jobName.toLowerCase().replaceAll("[^a-zA-Z0-9\\-_]", "_").replaceAll("__", "_");
    }
}
