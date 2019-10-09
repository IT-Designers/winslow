package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;

import javax.annotation.Nonnull;
import java.util.*;

public class JobBuilder {

    @Nonnull private static final String DRIVER_DOCKER = "docker";

    @Nonnull private final String                  id;
    @Nonnull private final Map<String, Object>     config    = new HashMap<>(10);
    @Nonnull private final Map<String, String>     envVars   = new HashMap<>(10);
    private                String                  taskName;
    private                String                  driver;
    private final          Resources               resources = new Resources();
    private                HashMap<String, Object> deviceGpu = null;

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
    public Optional<String> getEnvVariable(@Nonnull String key) {
        return Optional.ofNullable(this.envVars.get(key));
    }

    @Nonnull
    public JobBuilder withEnvVariableUnset(@Nonnull String key) {
        this.envVars.remove(key);
        return this;
    }

    @Nonnull
    public JobBuilder withEnvVariableSet(@Nonnull String key, @Nonnull String value) {
        this.envVars.put(key, value);
        return this;
    }

    @Nonnull
    public JobBuilder withEnvVariablesSet(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVars.putAll(variables);
        return this;
    }

    @Nonnull
    public JobBuilder withGpuCount(int count) {
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
    public JobBuilder withGpuVendor(@Nonnull String vendor) {
        ensureGpuDeviceAdded();
        this.deviceGpu.put("Name", vendor + "/gpu");
        return this;
    }

    private void ensureGpuDeviceAdded() {
        if (this.deviceGpu == null) {
            this.deviceGpu = new HashMap<>();
            if (this.resources.getUnmappedProperties() != null) {
                ((List<Object>) this.resources.getUnmappedProperties().computeIfAbsent("Devices", key -> new ArrayList<>())).add(deviceGpu);
            } else {
                var list = new ArrayList<>();
                list.add(deviceGpu);
                this.resources.addUnmappedProperty("Devices", list);
            }
        }
    }

    @Nonnull
    public Job buildJob() {
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
                                .setResources(resources)
                                .setEnv(this.envVars)));
    }


    private void ensureDriverDocker() {
        if (driver != null && !DRIVER_DOCKER.equals(driver)) {
            throw new IllegalStateException("Driver is already set to '' but require it to be set to ''");
        }
        this.driver = DRIVER_DOCKER;
    }
}
