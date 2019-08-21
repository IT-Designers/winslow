package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class NomadOrchestrator implements Orchestrator {

    private final NomadApiClient client;

    public NomadOrchestrator(NomadApiClient client) {
        Objects.requireNonNull(client);
        this.client = client;

        try {
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
    public RunningStage start(Pipeline pipeline, Stage stage, Environment environment) throws OrchestratorException {
        var builder = SubmissionBuilder
                .withRandomUuid()
                .withTaskName(combine(pipeline.getName(), stage.getName()));

        if (stage.getImage().isPresent()) {
            builder = builder
                    .withDockerImage(stage.getImage().get().getName())
                    .withDockerImageArguments(stage.getImage().get().getArguments());
        }

        if (environment.getWorkDirectoryConfiguration() instanceof NfsWorkDirectory) {
            var config = (NfsWorkDirectory) environment.getWorkDirectoryConfiguration();
            var resources = environment.getResourceManager().getResourceDirectory();
            var workspace = environment.getResourceManager().createWorkspace(builder.getUuid(), true);

            if (resources.isEmpty() || workspace.isEmpty()) {
                workspace.map(Path::toFile).map(File::delete);
                throw new OrchestratorException("The workspace and resources directory must exit, but at least one isn't. workspace="+workspace+",resources="+resources);
            }

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

        try {
            return builder.submit(this, pipeline, stage, environment);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Connection to nomad failed", e);
        } catch (NomadException e) {
            throw new OrchestratorException("Internal error", e);
        }
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
}
