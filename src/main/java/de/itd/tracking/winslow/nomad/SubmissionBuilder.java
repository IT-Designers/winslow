package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.apimodel.RestartPolicy;
import com.hashicorp.nomad.apimodel.Task;
import com.hashicorp.nomad.apimodel.TaskGroup;
import com.hashicorp.nomad.javasdk.JobsApi;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.io.IOException;
import java.util.*;

public class SubmissionBuilder {

    public static final String DRIVER_DOCKER = "docker";

    private final UUID                uuid;
    private       String              taskName;
    private       String              driver;
    private       Map<String, Object> config = new HashMap<>();

    private SubmissionBuilder(UUID uuid) {
        this.uuid = uuid;
    }

    public static SubmissionBuilder withRandomUuid() {
        return new SubmissionBuilder(UUID.randomUUID());
    }

    public UUID getUuid() {
        return uuid;
    }

    public SubmissionBuilder withTaskName(String name) {
        this.taskName = name;
        return this;
    }

    public String getTaskName() {
        return taskName;
    }

    public SubmissionBuilder withDockerImage(String image) {
        this.ensureDriverDocker();
        this.config.put("image", image);
        return this;
    }

    public SubmissionBuilder withDockerImageArguments(String...args) {
        this.ensureDriverDocker();
        this.config.put("args", args);
        return this;
    }

    public SubmissionBuilder addNfsVolume(String volumeName, String target, boolean readonly, String options, String serverExport) {
        var list = (List<Map<String, Object>>)this.config.computeIfAbsent("mounts", (s) -> new ArrayList<Map<String, Object>>());
        list.add(Map.of(
                "type", "volume",
                "target", target,
                "source", volumeName,
                "readonly", readonly,
                "volume_options", List.of(Map.<String, Object>of(
                        "driver_config", Map.of(
                                "name", "local",
                                "options", List.of(Map.<String, Object>of(
                                        "type", "nfs",
                                        "o", options,
                                        "device", ":"+serverExport
                                ))
                        )
                ))
        ));
        return this;
    }

    public Job buildJob(Pipeline pipeline, Stage stage, Environment env) {
        return new Job()
                .setId(this.uuid.toString())
                .addDatacenters("local")
                .setType("batch")
                .addTaskGroups(
                        new TaskGroup()
                                .setName(taskName)
                                .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                .addTasks(
                                        new Task()
                                                .setName(taskName)
                                                .setDriver(driver)
                                                .setConfig(config)
                                                .setEnv(stage.getEnvironment())
                                )
                );
    }

    public Submission submit(NomadOrchestrator orchestrator, Pipeline pipeline, Stage stage, Environment environment) throws IOException, NomadException {
        orchestrator.getClient().getJobsApi().register(buildJob(pipeline, stage, environment));
        return new Submission(orchestrator, uuid.toString(), taskName);
    }


    private void ensureDriverDocker() {
        if (driver != null && !DRIVER_DOCKER.equals(driver)) {
            throw new IllegalStateException("Driver is already set to '' but require it to be set to ''");
        }
        this.driver = DRIVER_DOCKER;
    }
}
