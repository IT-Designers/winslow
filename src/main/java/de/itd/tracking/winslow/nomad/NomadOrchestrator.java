package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.RunningStage;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadOrchestrator implements Orchestrator {

    private static final Logger LOG = Logger.getLogger(NomadOrchestrator.class.getSimpleName());

    @Nonnull private final NomadApiClient client;
    @Nonnull private final NomadRepository repository;

    public NomadOrchestrator(@Nonnull NomadApiClient client, @Nonnull NomadRepository repository) {
        this.client = client;
        this.repository = repository;


        try {
            client.getNodesApi().list().getValue().forEach(node -> {
                System.out.println(node.getAddress());
                try {
                    NetworkInterface.networkInterfaces().forEach(nic -> {
                        try {
                            var nodeAddress = InetAddress.getByName(node.getAddress());
                            var enumeration = nic.getInetAddresses();

                            while (enumeration.hasMoreElements()) {
                                var inet = enumeration.nextElement();
                                if (inet.equals(nodeAddress)) {
                                    System.out.println("nic:" + nic.getName());
                                    System.out.println("    " + inet);
                                    nic.getInterfaceAddresses().forEach(inter -> System.out.println("      : " + inter));
                                }
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            });



            client.getSystemApi().garbageCollect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        }
    }

    private String combine(String pipelineName, String stageName) {
        return String.format("%s-%s", pipelineName, stageName);
    }

    @Nonnull
    @Override
    public Optional<RunningStage> getCurrentlyRunningStage(@Nonnull Project project) {
        return repository
                .getNomadProject(project.getId())
                .locked()
                .flatMap(locked -> {
                    try (locked) {
                        return locked.get().map(nomadProject -> nomadProject.toSubmission(this));
                    } catch (LockException e) {
                        LOG.log(Level.SEVERE, "Failed to lock nomad project file", e);
                        return Optional.empty();
                    }
                });
    }

    @Nonnull
    @Override
    public Optional<RunningStage> startNextStage(@Nonnull Project project, @Nonnull Environment environment) {
        var index = project.getNextStageIndex();
        var stages = project.getPipeline().getStages();

        if (index >= 0 && index < stages.size()) {
            try {
                return prepare(project.getPipeline(), stages.get(index), environment).start().flatMap(s -> repository.getNomadProject(project.getId()).locked().flatMap(locked -> {
                    if (s instanceof Submission) {
                        try (locked) {
                            var nomadProject = new NomadProject(((Submission) s).getJobId(), ((Submission) s).getTaskName());
                            locked.update(nomadProject);
                            project.setNextStageIndex(index + 1);
                            return Optional.of(nomadProject.toSubmission(this));
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, "Failed to update nomad project file", e);
                        }
                    }
                    return Optional.empty();
                }));

            } catch (OrchestratorException e) {
                e.printStackTrace(); // TODO
            }
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public PreparedSubmission prepare(Pipeline pipeline, Stage stage, Environment environment) throws OrchestratorException {
        var builder = JobBuilder
                .withRandomUuid()
                .withTaskName(replaceInvalidCharactersInJobName(combine(pipeline.getName(), stage.getName())));

        var resources = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(builder.getUuid(), true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new OrchestratorException("The workspace and resources directory must exit, but at least one isn't. workspace="+workspace+",resources="+resources);
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
                throw new OrchestratorException("The workspace and resource path must be exported, but at least one isn't. workspace="+exportedWorkspace+",resources="+exportedResources);
            }

            System.out.println(resources);
            System.out.println(workspace);
            System.out.println(exportedResources);
            System.out.println(exportedWorkspace);

            builder = builder
                    .addNfsVolume(
                            "winslow-"+builder.getUuid()+"-resources",
                            "/resources",
                            true,
                            config.getOptions(),
                            exportedResources.get().toAbsolutePath().toString()
                    )
                    .addNfsVolume(
                            "winslow-"+builder.getUuid()+"-workspace",
                            "/workspace",
                            false,
                            config.getOptions(),
                            exportedWorkspace.get().toAbsolutePath().toString()
                    );
        } else {
            throw new OrchestratorException("Unknown WorkDirectoryConfiguration: " + environment.getWorkDirectoryConfiguration());
        }

        return new PreparedSubmission(
                builder.buildJob(pipeline, stage, environment),
                this,
                workspace.get()
        );
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
                if (allocationListStub.getTaskStates() != null && allocationListStub.getTaskStates().get(taskName) != null) {
                    return Optional.of(allocationListStub);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Boolean> hasTaskStarted(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates().get(taskName)).map(state -> state.getStartedAt().after(new Date(1)));
    }

    public static Optional<Boolean> hasTaskFinished(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates().get(taskName)).map(state -> state.getFinishedAt().after(new Date(1)));
    }

    public static Optional<Boolean> hasTaskFailed(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates().get(taskName)).map(TaskState::getFailed);
    }

    public static Optional<RunningStage.State> toRunningStageState(AllocationListStub allocation, String taskName) {
        var failed = hasTaskFailed(allocation, taskName);
        var started = hasTaskStarted(allocation, taskName);
        var finished = hasTaskFinished(allocation, taskName);

        if (failed.isPresent() && failed.get()) {
            return Optional.of(RunningStage.State.Failed);
        } else if (started.isPresent() && !started.get()) {
            return Optional.of(RunningStage.State.Preparing);
        } else if (started.isPresent() && finished.isPresent() && !finished.get()) {
            return Optional.of(RunningStage.State.Running);
        } else if (finished.isPresent() && finished.get()) {
            return Optional.of(RunningStage.State.Succeeded);
        } else {
            return Optional.empty();
        }
    }


    public static String replaceInvalidCharactersInJobName(String jobName) {
        return jobName.replaceAll("^[a-zA-Z0-9]", "-");
    }

}
