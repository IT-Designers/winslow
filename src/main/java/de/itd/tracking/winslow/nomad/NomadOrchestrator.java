package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class NomadOrchestrator implements Orchestrator {

    private final NomadApiClient client;

    public NomadOrchestrator(NomadApiClient client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    private String combine(String pipelineName, String stageName) {
        return String.format("%s-%s", pipelineName, stageName);
    }

    @Override
    public String start(Pipeline pipeline, Stage stage, Environment environment) {
        var uuid = UUID.randomUUID();
        var name = combine(pipeline.getName(), stage.getName());

        var dockerConfig = new HashMap<String, Object>();
        stage.getImage().ifPresent(image -> {
            dockerConfig.put("image", image.getName());
            dockerConfig.put("args", image.getArguments());
        });


        var dev = new HashMap<String, Object>();
        /*
        dev.put("Name", "nvidia/gpu");
        dev.put("Count", 1);
        */

        var res = new Resources();
        //res.addUnmappedProperty("Devices", Arrays.asList(dev));

        try {
            client.getJobsApi().register(
                    new Job()
                            .setId(uuid.toString())
                            .addDatacenters("local")
                            .setType("batch")
                            .addTaskGroups(
                                    new TaskGroup()
                                            .setName(name)
                                            .addTasks(
                                                    new Task()
                                                            .setName(name)
                                                            .setDriver("docker")
                                                            .setConfig(dockerConfig)
                                                            .setEnv(stage.getEnvironment())
                                                            .setResources(
                                                                    res
                                                            )
                                            )
                            )
            );
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


            //client.getSystemApi().garbageCollect();
            return uuid.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } return null;
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
