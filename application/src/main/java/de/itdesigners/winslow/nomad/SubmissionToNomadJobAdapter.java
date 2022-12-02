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

    private final @Nonnull String       nodeName;
    private final @Nonnull PlatformInfo info;
    private final @Nonnull NomadBackend backend;

    public SubmissionToNomadJobAdapter(
            @Nonnull String nodeName,
            @Nonnull PlatformInfo info,
            @Nonnull NomadBackend backend
    ) {
        this.nodeName = nodeName;
        this.info     = info;
        this.backend  = backend;
    }

    @Nonnull
    public SubmissionResult submit(@Nonnull Submission submission) throws OrchestratorException, IOException, NomadException {
        try {
            submission.ensureNotSubmittedYet();
        } catch (Submission.AlreadySubmittedException e) {
            throw new OrchestratorException("Submission already submitted", e);
        }

        var job      = createJob(submission);
        var jobId    = job.getId();
        var taskName = job.getTaskGroups().get(0).getName();
        var stage    = submission.createStage();
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
                return new SubmissionResult(stage, new NomadStageHandle(nodeName, backend, submission.getId()));
            }
        }
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
        submission.getExtension(DockerVolumes.class).ifPresent(getDockerNfsVolumesConfigurer(task));
        getResourceRequirementsConfigurer(task);


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

    @Nonnull
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
            task.getConfig().put("image_pull_timeout", "1h");

            docker.getShmSizeMegabytes().ifPresent(shm -> {
                task.getConfig().put("shm_size", shm * 1024L * 1024L);
                task.getConfig().put("ulimit", List.of(Map.of("memlock", -1)));
            });

        };
    }

    @Nonnull
    @CheckReturnValue
    public static Consumer<DockerVolumes> getDockerNfsVolumesConfigurer(Task task) {
        return list -> {
            var configList = (List<Map<String, Object>>) task.getConfig().computeIfAbsent(
                    "mounts",
                    (s) -> new ArrayList<Map<String, Object>>()
            );
            for (var volume : list.getVolumes()) {
                configList.add(getMount(volume).orElseThrow());
            }
        };
    }

    @Nonnull
    private static Optional<Map<String, Object>> getMount(@Nonnull DockerVolume volume) {
        return switch (volume.getType().toLowerCase()) {
            case "nfs" -> Optional.of(getNfsVolumeMount(volume));
            case "bind" -> Optional.of(getBindVolumeMount(volume));
            default -> Optional.empty();
        };
    }

    @Nonnull
    private static Map<String, Object> getNfsVolumeMount(@Nonnull DockerVolume volume) {
        return Map.of(
                "type", "volume",
                "target", volume.getContainerPath(),
                "source", volume.getName(),
                "readonly", volume.isReadonly(),
                "volume_options",
                List.of(Map.<String, Object>of(
                        "driver_config",
                        Map.of(
                                "name", "local",
                                "options", List.of(Map.<String, Object>of(
                                        "type", "nfs",
                                        "o", volume.getOptions(),
                                        "device", volume.getHostPath()
                                ))
                        )
                ))
        );
    }

    @Nonnull
    private static Map<String, Object> getBindVolumeMount(@Nonnull DockerVolume volume) {
        return Map.of(
                "type", "bind",
                "target", volume.getContainerPath(),
                "source", volume.getHostPath(),
                "readonly", volume.isReadonly()
        );
    }

    @Nonnull
    @CheckReturnValue
    private Consumer<Requirements> getResourceRequirementsConfigurer(@Nonnull Task task) {
        return requirements -> {
            if (requirements.getGpu().getCount() > 0) {
                var gpuDevice = new RequestedDevice();
                gpuDevice.setName("gpu");
                gpuDevice.setCount(BigInteger.valueOf(requirements.getGpu().getCount()));

                var vendor = requirements
                        .getGpu()
                        .getVendor()
                        .orElse(NomadBackend.DEFAULT_GPU_VENDOR);
                gpuDevice.setName(vendor + "/gpu");

                task.getResources().addDevices(gpuDevice);
            }

            if (requirements.getCpus() > 0) {
                info.getCpuSingleCoreMaxFrequency()
                    .ifPresent(max -> {
                        // TODO magic number, I dont know why, but somehow this is necessary
                        var compute = ((requirements.getCpus() * max) - NOMAD_SYSTEM_RESERVED_CPU) / 3.5f;
                        if (compute > NOMAD_MIN_RESERVABLE_CPU) {
                            task.getResources().setCpu((int) compute);
                        }
                    });
            }

            task.getResources().setMemoryMb((int) requirements.getMegabytesOfRam());
        };
    }
}
