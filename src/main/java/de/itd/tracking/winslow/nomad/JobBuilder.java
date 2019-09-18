package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.apimodel.RestartPolicy;
import com.hashicorp.nomad.apimodel.Task;
import com.hashicorp.nomad.apimodel.TaskGroup;
import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.*;

public class JobBuilder {

    @Nonnull private static final String DRIVER_DOCKER = "docker";

    @Nonnull private final String              id;
    @Nonnull private final Map<String, Object> config = new HashMap<>();
    private                String              taskName;
    private                String              driver;

    private JobBuilder(@Nonnull String id) {
        this.id = id;
    }

    @Nonnull
    public static JobBuilder withRandomUuid() {
        return new JobBuilder(UUID.randomUUID().toString());
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public JobBuilder withTaskName(String name) {
        this.taskName = name;
        return this;
    }

    public String getTaskName() {
        return taskName;
    }

    @Nonnull
    public JobBuilder withDockerImage(String image) {
        this.ensureDriverDocker();
        this.config.put("image", image);
        return this;
    }

    @Nonnull
    public JobBuilder withDockerImageArguments(String... args) {
        this.ensureDriverDocker();
        this.config.put("args", args);
        return this;
    }

    @Nonnull
    public JobBuilder addNfsVolume(String volumeName, String target, boolean readonly, String options, String serverExport) {
        var list = (List<Map<String, Object>>) this.config.computeIfAbsent("mounts", (s) -> new ArrayList<Map<String, Object>>());
        list.add(Map.of("type", "volume", "target", target, "source", volumeName, "readonly", readonly, "volume_options", List
                .of(Map.<String, Object>of("driver_config", Map.of("name", "local", "options", List.of(Map.<String, Object>of("type", "nfs", "o", options, "device", ":" + serverExport)))))));
        return this;
    }

    @Nonnull
    public Job buildJob(PipelineDefinition pipelineDefinition, StageDefinition stageDefinition, Environment env) {
        return new Job()
                .setId(this.id)
                .addDatacenters("local")
                .setType("batch")
                .addTaskGroups(new TaskGroup()
                        .setName(taskName)
                        .setRestartPolicy(new RestartPolicy().setAttempts(0))
                        .addTasks(new Task()
                                .setName(taskName)
                                .setDriver(driver)
                                .setConfig(config)
                                .setEnv(stageDefinition.getEnvironment())));
    }


    private void ensureDriverDocker() {
        if (driver != null && !DRIVER_DOCKER.equals(driver)) {
            throw new IllegalStateException("Driver is already set to '' but require it to be set to ''");
        }
        this.driver = DRIVER_DOCKER;
    }
}
