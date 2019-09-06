package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadOrchestrator implements Orchestrator {

    private static final Logger LOG = Logger.getLogger(NomadOrchestrator.class.getSimpleName());

    @Nonnull private final NomadApiClient  client;
    @Nonnull private final NomadRepository repository;

    public NomadOrchestrator(@Nonnull NomadApiClient client, @Nonnull NomadRepository repository) {
        this.client     = client;
        this.repository = repository;
    }

    private String combine(String pipelineName, String stageName) {
        return String.format("%s-%s", pipelineName, stageName);
    }

    @Nonnull
    @Override
    public Optional<Submission> getCurrent(@Nonnull Project project) {
        return getCurrentAllocation(project).map(allocatedJob -> allocatedJob);
    }

    @Override
    public boolean canProgress(@Nonnull Project project) {
        var job = getCurrentAllocation(project);
        return canProgressAutomatically(project, job);
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
        var job = repository.getNomadProject(project.getId()).unsafe().map(j -> j.getAllocation(this));
        return canProgressAutomatically(project, job);
    }

    private boolean canProgressAutomatically(@Nonnull Project project, @Nonnull Optional<AllocatedJob> job) {
        return (job.isEmpty() || hasCompletedSuccessfully(job.get())) && project.getNextStageIndex() < project
                .getPipeline()
                .getStages()
                .size();
    }

    @Nonnull
    public Optional<AllocatedJob> getCurrentAllocation(@Nonnull Project project) {
        return repository.getNomadProject(project.getId()).locked().flatMap(locked -> {
            try (locked) {
                return locked.get().map(nomadProject -> nomadProject.getAllocation(this));
            } catch (LockException e) {
                LOG.log(Level.SEVERE, "Failed to lock nomad project file", e);
                return Optional.empty();
            }
        });
    }

    @Nonnull
    @Override
    public Optional<Submission> startNext(@Nonnull Project project, @Nonnull Environment environment) {
        var index  = project.getNextStageIndex();
        var stages = project.getPipeline().getStages();

        if (index < 0 || index >= stages.size()) {
            return Optional.empty();
        }

        return repository.getNomadProject(project.getId()).locked().flatMap(locked -> {
            try (locked) {
                if (!canProgressAutomatically(project, locked.get().map(p -> p.getAllocation(this)))) {
                    return Optional.empty();
                }

                var stage    = stages.get(index);
                var prepared = prepare(project.getPipeline(), stage, environment);
                var running  = prepared.start();

                if (running.isPresent()) {
                    var jobId    = running.get().getJobId();
                    var taskName = running.get().getTaskName();
                    var nomad    = new NomadProject(jobId, taskName);

                    project.setNextStageIndex(index + 1);
                    locked.update(nomad);

                    return Optional.of(nomad.getAllocation(this));
                }
            } catch (OrchestratorException | IOException | LockException e) {
                LOG.log(Level.SEVERE, "Failed to start next stage for project " + project.getId(), e);
            }
            return Optional.empty();
        });
    }

    @Nonnull
    private PreparedJob prepare(Pipeline pipeline, Stage stage, Environment environment) throws OrchestratorException {
        var builder = JobBuilder
                .withRandomUuid()
                .withTaskName(replaceInvalidCharactersInJobName(combine(pipeline.getName(), stage.getName())));

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(builder.getUuid(), true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new OrchestratorException("The workspace and resources directory must exit, but at least one isn't. workspace=" + workspace + ",resources=" + resources);
        }

        if (stage.getImage().isPresent()) {
            builder = builder
                    .withDockerImage(stage.getImage().get().getName())
                    .withDockerImageArguments(stage.getImage().get().getArguments());
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

        return new PreparedJob(builder.buildJob(pipeline, stage, environment), this);
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

    public static Optional<Submission.State> toRunningStageState(AllocationListStub allocation, String taskName) {
        var failed   = hasTaskFailed(allocation, taskName);
        var started  = hasTaskStarted(allocation, taskName);
        var finished = hasTaskFinished(allocation, taskName);

        if (failed.isPresent() && failed.get()) {
            return Optional.of(Submission.State.Failed);
        } else if (started.isPresent() && !started.get()) {
            return Optional.of(Submission.State.Preparing);
        } else if (started.isPresent() && finished.isPresent() && !finished.get()) {
            return Optional.of(Submission.State.Running);
        } else if (finished.isPresent() && finished.get()) {
            return Optional.of(Submission.State.Succeeded);
        } else {
            return Optional.empty();
        }
    }


    public static String replaceInvalidCharactersInJobName(String jobName) {
        return jobName.replaceAll("[^a-zA-Z0-9]", "_");
    }

}
