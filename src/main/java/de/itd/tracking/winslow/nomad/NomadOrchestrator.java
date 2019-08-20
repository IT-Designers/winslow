package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.NfsConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
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

    @Override
    public String start(Pipeline pipeline, Stage stage, Environment environment) {
        var builder = SubmissionBuilder
                .withRandomUuid()
                .withTaskName(combine(pipeline.getName(), stage.getName()));

        if (stage.getImage().isPresent()) {
            builder = builder
                    .withDockerImage(stage.getImage().get().getName())
                    .withDockerImageArguments(stage.getImage().get().getArguments());
        }

        if (environment.getConfiguration() instanceof NfsConfiguration) {
            var config = (NfsConfiguration) environment.getConfiguration();
            builder = builder
                    .addNfsVolume(
                            "winslow-resources",
                            "/resources",
                            true,
                            config.getOptions(),
                            config.getServerExport()+"/resources"
                    )
                    .addNfsVolume(
                            "winslow-workspace-"+builder.getUuid(),
                            "/workspace",
                            false,
                            config.getOptions(),
                            config.getServerExport()+"/workspaces/abc-def-uui"
                    );
        }

        try {

            var submission = builder.submit(this, pipeline, stage, environment);
            var uuid = UUID.fromString(submission.getJobId());
            var name = submission.getTaskName();

            System.out.println("beginning");
            for (String line : submission.getStdOut()) {
                System.out.print(line);
            }
            System.out.println("end");

            System.out.println("...");
            System.out.println(client.getJobsApi().info(uuid.toString()));
            System.out.println("---");
            FramedStream input = null;
            outer: while (true) {
                var allocation = getJobAllocationContainingTaskState(uuid.toString(), name);
                if (allocation.isPresent()) {
                    var a = allocation.get();
                    if (a.getTaskStates().get(name).getStartedAt().after(new Date(1))) {
                        if (input == null) {
                            input = client.getClientApi(client.getConfig().getAddress()).logsAsFrames(a.getId(), name, true, "stdout");
                        }
                        var frame = input.nextFrame();
                        if (frame != null && frame.getData() != null) {
                            System.out.write(frame.getData());
                        }
                    }
                    if (a.getTaskStates().get(name).getFinishedAt().after(new Date(1))) {
                        System.out.println("-- done");
                        a.getTaskStates().get(name).getEvents().forEach(System.out::println);
                        break;
                    } else {
                        var stats = client.getClientApi(client.getConfig().getAddress()).stats(a.getId()).getValue();
                        if (stats != null && stats.getResourceUsage() != null && stats.getResourceUsage().getMemoryStats() != null && stats.getResourceUsage().getMemoryStats().getMeasured() != null) {
                            //System.out.println(stats.getResourceUsage().getMemoryStats().getMaxUsage());
                        }
                    }
                }
                Thread.sleep(100);
            }



            return uuid.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } return null;
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
