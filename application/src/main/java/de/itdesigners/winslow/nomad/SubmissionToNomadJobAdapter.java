package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.NoOpStageHandle;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.node.PlatformInfo;
import de.itdesigners.winslow.pipeline.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

public class SubmissionToNomadJobAdapter {

    private static final @Nonnull String DOCKER_DRIVER             = "docker";
    private static final          int    NOMAD_MIN_RESERVABLE_CPU  = 100;
    private static final          int    NOMAD_SYSTEM_RESERVED_CPU = 150;

    private final @Nonnull PlatformInfo info;
    private final @Nonnull NomadBackend backend;

    public SubmissionToNomadJobAdapter(@Nonnull PlatformInfo info, @Nonnull NomadBackend backend) {
        this.info    = info;
        this.backend = backend;
    }

    public SubmissionResult submit(@Nonnull Submission submission) throws OrchestratorException, IOException, NomadException {
        if (submission.getResult().isPresent()) {
            throw new OrchestratorException("Submission already submitted");
        }

        var job      = createJob(submission);
        var jobId    = job.getId();
        var taskName = job.getTaskGroups().get(0).getName();
        var stage    = createStage(submission);
        var stageId  = stage.getFullyQualifiedId();

        if (!Objects.equals(jobId, taskName) || !Objects.equals(jobId, stageId)) {
            throw new OrchestratorException("Invalid configuration, jobId must match taskName, but doesn't: " + jobId + " != " + taskName);
        }

        if (submission.isConfigureOnly()) {
            stage.finishNow(State.Succeeded);
            return new SubmissionResult(stage, new NoOpStageHandle());
        } else {
            try (var client = backend.getNewClient()) {
                client.getJobsApi().register(job);
                return new SubmissionResult(stage, new NomadStageHandle(backend, submission.getId()));
            }
        }
    }

    @Nonnull
    @CheckReturnValue
    private Stage createStage(@Nonnull Submission submission) {
        var stage = new Stage(
                submission.getId(),
                submission.getWorkspaceDirectory().orElse(null)
        );
        stage.getEnv().putAll(submission.getStageEnvVariablesReduced());
        stage.getEnvPipeline().putAll(submission.getPipelineEnvVariables());
        stage.getEnvSystem().putAll(submission.getSystemEnvVariables());
        stage.getEnvInternal().putAll(submission.getInternalEnvVariables());

        stage.getEnvPipeline().forEach((key, value) -> stage.getEnv().remove(key, value));
        stage.getEnvSystem().forEach((key, value) -> {
            if (!stage.getEnvPipeline().containsKey(key)) {
                stage.getEnv().remove(key, value);
            }
        });
        stage.getEnvInternal().forEach((key, value) -> stage.getEnv().remove(key, value));
        return stage;
    }

    @Nonnull
    private Job createJob(@Nonnull Submission submission) {

        var task = new Task()
                .setName(submission.getId().getFullyQualified())
                .setEnv(getVisibleEnvironmentVariables(submission))
                .setConfig(new HashMap<>())
                .setResources(new Resources())
                .setRestartPolicy(new RestartPolicy().setAttempts(0));


        submission.getExtension(DockerImage.class).ifPresent(getDockerImageConfigurer(task));
        submission.getExtension(DockerNfsVolumes.class).ifPresent(getDockerNfsVolumesConfigurer(task));
        submission.getStageDefinition().getRequirements().ifPresent(getResourceRequirementsConfigurer(task));


        return new Job()
                .setId(submission.getId().getFullyQualified())
                .addDatacenters("local")
                .setType("batch")
                .setReschedule(new ReschedulePolicy().setAttempts(0))
                .addTaskGroups(
                        new TaskGroup()
                                .setName(submission.getId().getFullyQualified())
                                .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                .addTasks(task)
                );
    }

    private HashMap<String, String> getVisibleEnvironmentVariables(@Nonnull Submission submission) {
        var env = new HashMap<String, String>();

        submission
                .getEnvVariableKeys()
                .forEach(key -> submission.getEnvVariable(key).ifPresent(value -> env.put(key, value)));
        return env;
    }

    @Nonnull
    @CheckReturnValue
    private static Consumer<DockerImage> getDockerImageConfigurer(Task task) {
        return docker -> {
            task.setDriver(DOCKER_DRIVER);
            task.getConfig().put("image", docker.getImage());
            task.getConfig().put("args", docker.getArguments());
            task.getConfig().put("privileged", docker.isPrivileged());

            docker.getShmSizeMegabytes().ifPresent(shm -> {
                task.getConfig().put("shm_size", shm * 1024L * 1024L);
                task.getConfig().put("ulimit", List.of(Map.of("memlock", -1)));
            });

        };
    }

    @Nonnull
    @CheckReturnValue
    public static Consumer<DockerNfsVolumes> getDockerNfsVolumesConfigurer(Task task) {
        return list -> {
            var configList = (List<Map<String, Object>>) task.getConfig().computeIfAbsent(
                    "mounts",
                    (s) -> new ArrayList<Map<String, Object>>()
            );
            for (var volume : list.getVolumes()) {
                configList.add(Map.of(
                        "type", "volume",
                        "target", volume.getTargetPath(),
                        "source", volume.getName(),
                        "readonly", volume.isReadonly(),
                        "volume_options",
                        List.of(Map.<String, Object>of(
                                "driver_config",
                                Map.of(
                                        "name", "local",
                                        "options", List.of(Map.<String, Object>of(
                                                "type",
                                                "nfs",
                                                "o",
                                                volume.getOptions(),
                                                "device",
                                                ":" + volume.getServerPath()
                                        ))
                                )
                        ))
                ));
            }
        };
    }

    @Nonnull
    @CheckReturnValue
    private Consumer<Requirements> getResourceRequirementsConfigurer(@Nonnull Task task) {
        return requirements -> {
            requirements.getGpu().ifPresent(gpu -> {
                if (gpu.getCount() > 0) {
                    var gpuDevice = new RequestedDevice();
                    gpuDevice.setName("gpu");
                    gpuDevice.setCount(BigInteger.valueOf(gpu.getCount()));

                    gpu.getVendor().ifPresent(vendor -> {
                        gpuDevice.setName(vendor + "/gpu");
                    });

                    task.getResources().addDevices(gpuDevice);
                }

            });

            if (requirements.getCpu() > 0) {
                info.getCpuSingleCoreMaxFrequency()
                    .ifPresent(max -> {
                        var compute = (requirements.getCpu() * max) - NOMAD_SYSTEM_RESERVED_CPU;
                        if (compute > NOMAD_MIN_RESERVABLE_CPU) {
                            task.getResources().setCpu(compute);
                        }
                    });
            }

            task.getResources().setMemoryMb((int) requirements.getMegabytesOfRam());
        };
    }
}
