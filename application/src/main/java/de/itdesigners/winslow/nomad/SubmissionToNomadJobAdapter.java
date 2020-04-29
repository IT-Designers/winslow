package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.pipeline.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class SubmissionToNomadJobAdapter {

    private static final @Nonnull String DOCKER_DRIVER = "docker";

    private final @Nonnull NomadBackend backend;

    public SubmissionToNomadJobAdapter(@Nonnull NomadBackend backend) {
        this.backend = backend;
    }

    public SubmissionResult submit(@Nonnull Submission submission) throws OrchestratorException, IOException, NomadException {
        if (submission.getResult().isPresent()) {
            throw new OrchestratorException("Submission already submitted");
        }

        var job      = createJob(submission);
        var jobId    = job.getId();
        var taskName = job.getTaskGroups().get(0).getName();
        var stage    = createStage(submission, jobId);

        if (!Objects.equals(jobId, taskName)) {
            throw new OrchestratorException("Invalid configuration, jobId must match taskName, but doesn't: " + jobId + " != " + taskName);
        }

        switch (submission.getAction()) {
            case Execute:
                // this one could fail
                backend.getNewJobsApi().register(job);
                break;
            case Configure:
                // a configure is successful by being instantiated and has no lifetime
                stage.finishNow(State.Succeeded);
                break;
        }

        return new SubmissionResult(stage, new NomadStageHandle(backend, jobId));
    }

    @Nonnull
    @CheckReturnValue
    private Stage createStage(@Nonnull Submission submission, @Nonnull String jobId) {
        var stage = new Stage(
                jobId,
                submission.getStageDefinition(),
                submission.getAction(),
                submission.getWorkspaceDirectory().orElseThrow()
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
                .setName(submission.getId())
                .setEnv(getVisibleEnvironmentVariables(submission))
                .setConfig(new HashMap<>())
                .setResources(new Resources());


        submission.getExtension(DockerImage.class).ifPresent(getDockerImageConfigurer(task));
        submission.getExtension(DockerNfsVolumes.class).ifPresent(getDockerNfsVolumesConfigurer(task));
        submission.getStageDefinition().getRequirements().ifPresent(getResourceRequirementsConfigurer(task));


        return new Job()
                .setId(submission.getId())
                .addDatacenters("local")
                .setType("batch")
                .addTaskGroups(
                        new TaskGroup()
                                .setName(submission.getId())
                                .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                .addTasks(task));

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
            // TODO requirements.getCpu()
            requirements.getGpu().ifPresent(gpu -> {
                if (gpu.getCount() > 0) {
                    var configGpu = new HashMap<>();
                    configGpu.put("Count", gpu.getCount());
                    configGpu.put("Name", "gpu");

                    gpu.getVendor().ifPresent(vendor -> {
                        configGpu.put("Name", vendor + "/gpu");
                    });

                    if (task.getResources().getUnmappedProperties() == null) {
                        task.getResources().addUnmappedProperty("Devices", new ArrayList<>(List.of(configGpu)));
                    } else {
                        ((List<Object>) task.getResources().getUnmappedProperties().computeIfAbsent(
                                "Devices",
                                key -> new ArrayList<>()
                        )).add(configGpu);
                    }
                }

            });

            task.getResources().setMemoryMb((int) requirements.getMegabytesOfRam());
        };
    }
}
