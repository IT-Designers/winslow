package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.JobsApi;
import de.itd.tracking.winslow.pipeline.PreparedStageBuilder;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.*;

public class NomadPreparedStageBuilder implements PreparedStageBuilder {


    @Nonnull private static final String DRIVER_DOCKER = "docker";

    @Nonnull private final String                  id;
    @Nonnull private final JobsApi                 jobsApi;
    @Nonnull private final StageDefinition         stageDefinition;
    @Nonnull private final Map<String, Object>     config          = new HashMap<>();
    @Nonnull private final Map<String, String>     envVars         = new HashMap<>();
    @Nonnull private final Map<String, String>     envVarsInternal = new HashMap<>();
    private                String                  stage;
    private                String                  driver;
    private final          Resources               resources       = new Resources();
    private                HashMap<String, Object> deviceGpu       = null;
    private                String                  workspaceWithinPipeline;

    public NomadPreparedStageBuilder(
            @Nonnull String id,
            @Nonnull String stage,
            @Nonnull JobsApi jobsApi,
            @Nonnull StageDefinition stageDefinition) {
        this.id              = id;
        this.stage           = stage;
        this.jobsApi         = jobsApi;
        this.stageDefinition = stageDefinition;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public NomadPreparedStageBuilder withStage(@Nonnull String name) {
        this.stage = name;
        return this;
    }

    @Nonnull
    public String getStage() {
        return stage;
    }

    @Nonnull
    public NomadPreparedStageBuilder withDockerImage(String image) {
        this.ensureDriverDocker();
        this.config.put("image", image);
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder withDockerImageArguments(String... args) {
        this.ensureDriverDocker();
        this.config.put("args", args);
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder addNfsVolume(
            @Nonnull String volumeName,
            @Nonnull String target,
            boolean readonly,
            @Nonnull String options,
            @Nonnull String serverExport) {
        var list = (List<Map<String, Object>>) this.config.computeIfAbsent(
                "mounts",
                (s) -> new ArrayList<Map<String, Object>>()
        );
        list.add(Map.of(
                "type", "volume",
                "target", target,
                "source", volumeName,
                "readonly", readonly,
                "volume_options",
                List.of(Map.<String, Object>of(
                        "driver_config",
                        Map.of(
                                "name", "local",
                                "options", List.of(Map.<String, Object>of(
                                        "type",
                                        "nfs",
                                        "o",
                                        options,
                                        "device",
                                        ":" + serverExport
                                ))
                        )
                ))
        ));
        return this;
    }

    @Nonnull
    public Optional<String> getEnvVariable(@Nonnull String key) {
        return Optional.ofNullable(this.envVars.get(key));
    }

    @Nonnull
    public NomadPreparedStageBuilder withEnvVariableUnset(@Nonnull String key) {
        this.envVars.remove(key);
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder withInternalEnvVariable(@Nonnull String key, @Nonnull String value) {
        this.envVarsInternal.put(key, value);
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder withEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVars.putAll(variables);
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder withGpuCount(int count) {
        this.ensureGpuDeviceAdded();
        if (count > 0) {
            this.deviceGpu.put("Count", count);
            this.deviceGpu.putIfAbsent("Name", "gpu");
        } else {
            this.deviceGpu.remove("Count");
        }
        return this;
    }

    @Nonnull
    public NomadPreparedStageBuilder withGpuVendor(@Nonnull String vendor) {
        ensureGpuDeviceAdded();
        this.deviceGpu.put("Name", vendor + "/gpu");
        return this;
    }

    private void ensureGpuDeviceAdded() {
        if (this.deviceGpu == null) {
            this.deviceGpu = new HashMap<>();
            if (this.resources.getUnmappedProperties() != null) {
                ((List<Object>) this.resources.getUnmappedProperties().computeIfAbsent(
                        "Devices",
                        key -> new ArrayList<>()
                )).add(deviceGpu);
            } else {
                var list = new ArrayList<>();
                list.add(deviceGpu);
                this.resources.addUnmappedProperty("Devices", list);
            }
        }
    }

    @Nonnull
    public NomadPreparedStage build() {
        var env = new HashMap<String, String>();
        env.putAll(this.envVars);
        env.putAll(this.envVarsInternal);
        return new NomadPreparedStage(
                new Job()
                        .setId(this.stage)
                        .addDatacenters("local")
                        .setType("batch")
                        .addTaskGroups(
                                new TaskGroup()
                                        .setName(this.stage)
                                        .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                        .addTasks(
                                                new Task()
                                                        .setName(this.stage)
                                                        .setDriver(this.driver)
                                                        .setConfig(this.config)
                                                        .setResources(this.resources)
                                                        .setEnv(env))),
                jobsApi,
                stageDefinition,
                workspaceWithinPipeline,
                envVars,
                envVarsInternal
        );
    }

    @Nonnull
    @Override
    public PreparedStageBuilder withWorkspaceWithinPipeline(@Nonnull String workspace) {
        this.workspaceWithinPipeline = workspace;
        return this;
    }


    private void ensureDriverDocker() {
        if (driver != null && !DRIVER_DOCKER.equals(driver)) {
            throw new IllegalStateException("Driver is already set to '' but require it to be set to ''");
        }
        this.driver = DRIVER_DOCKER;
    }
}
