package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.JobsApi;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.PreparedStageBuilder;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NomadPreparedStageBuilder implements PreparedStageBuilder {


    @Nonnull private static final String DRIVER_DOCKER = "docker";

    @Nonnull private final String                  id;
    @Nonnull private final JobsApi                 jobsApi;
    @Nonnull private final StageDefinition         stageDefinition;
    @Nonnull private final Map<String, Object>     config          = new HashMap<>();
    @Nonnull private final Map<String, String>     envVars         = new HashMap<>();
    @Nonnull private final Map<String, String>     envVarsPipeline = new HashMap<>();
    @Nonnull private final Map<String, String>     envVarsSystem   = new HashMap<>();
    @Nonnull private final Map<String, String>     envVarsInternal = new HashMap<>();
    private                String                  stage;
    private                String                  driver;
    private final          Resources               resources       = new Resources();
    private                HashMap<String, Object> deviceGpu       = null;
    private                String                  workspaceDirectory;

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
    @Override
    public Iterable<String> getEnvVariableKeys() {
        return Stream
                .of(
                        this.envVarsSystem.keySet().stream(),
                        this.envVarsPipeline.keySet().stream(),
                        this.envVars.keySet().stream(),
                        this.envVarsInternal.keySet().stream()
                )
                .flatMap(v -> v)
                .filter(key -> getEnvVariable(key).isPresent())
                .collect(Collectors.toSet());
    }

    @Nonnull
    public Optional<String> getEnvVariable(@Nonnull String key) {
        // enable null-overwrite
        if (this.envVarsInternal.containsKey(key)) {
            return Optional.ofNullable(this.envVarsInternal.get(key));
        } else if (this.envVars.containsKey(key)) {
            return Optional.ofNullable(this.envVars.get(key));
        } else if (this.envVarsPipeline.containsKey(key)) {
            return Optional.ofNullable(this.envVarsPipeline.get(key));
        } else if (this.envVarsSystem.containsKey(key)) {
            return Optional.ofNullable(this.envVarsSystem.get(key));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public NomadPreparedStageBuilder withInternalEnvVariable(@Nonnull String key, @Nonnull String value) {
        this.envVarsInternal.put(key, value);
        return this;
    }

    @Nonnull
    @Override
    public PreparedStageBuilder withPipelineEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVarsPipeline.putAll(variables);
        return this;
    }

    @Nonnull
    @Override
    public PreparedStageBuilder withSystemEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVarsSystem.putAll(variables);
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

    @Nonnull
    public NomadPreparedStageBuilder withMegabytesOfRam(int megabytesOfRam) {
        this.resources.setMemoryMb(megabytesOfRam);
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
        getEnvVariableKeys().forEach(key -> getEnvVariable(key).ifPresent(value -> env.put(key, value)));

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
                workspaceDirectory,
                getReducedEnvVars(),
                envVarsPipeline,
                envVarsSystem,
                envVarsInternal
        );
    }

    private TreeMap<String, String> getReducedEnvVars() {
        var envVars = new TreeMap<String, String>();
        this.envVars
                .entrySet()
                .stream()
                .filter(e -> {
                    var key   = e.getKey();
                    var value = e.getValue();
                    return (!envVarsPipeline.containsKey(key) || !Objects.equals(value, envVarsPipeline.get(key)))
                            && (!envVarsSystem.containsKey(key) || !Objects.equals(value, envVarsSystem.get(key)));
                })
                .forEach(entry -> envVars.put(entry.getKey(), entry.getValue()));
        return envVars;
    }

    @Nonnull
    @Override
    public PreparedStageBuilder withWorkspaceDirectory(@Nonnull String workspace) {
        this.workspaceDirectory = workspace;
        return this;
    }


    private void ensureDriverDocker() {
        if (driver != null && !DRIVER_DOCKER.equals(driver)) {
            throw new IllegalStateException("Driver is already set to '' but require it to be set to ''");
        }
        this.driver = DRIVER_DOCKER;
    }
}
