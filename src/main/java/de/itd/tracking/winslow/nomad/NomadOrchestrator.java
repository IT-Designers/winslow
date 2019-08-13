package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.*;
import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

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
    public String start(Pipeline pipeline, Stage stage) {
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
            client.getSystemApi().garbageCollect();
            return uuid.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        }
        return null;
    }
}
