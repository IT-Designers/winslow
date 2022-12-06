package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.model.*;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SubmissionToDockerContainerAdapter {

    private static final Logger LOG                = Logger.getLogger(SubmissionToDockerContainerAdapter.class.getSimpleName());
    private static final String DEFAULT_GPU_VENDOR = "nvidia";

    private final @Nonnull DockerBackend backend;

    public SubmissionToDockerContainerAdapter(@Nonnull DockerBackend backend) {
        this.backend = backend;
    }

    @Nonnull
    public SubmissionResult submit(@Nonnull Submission submission) throws OrchestratorException, IOException {
        try {
            submission.ensureNotSubmittedYet();
        } catch (Submission.AlreadySubmittedException e) {
            throw new OrchestratorException("Submission already submitted", e);
        }

        var stage         = submission.createStage();
        var stageId       = stage.getFullyQualifiedId();
        var containerName = backend.getContainerName(stageId);

        var imageExt = submission
                .getExtension(DockerImage.class)
                .orElseThrow(() -> new OrchestratorException("Missing DockerImage-Extension"));

        var createCmd = this.backend
                .getDockerClient()
                .createContainerCmd(imageExt.getImage())
                .withCmd(imageExt.getArguments())
                .withName(containerName)
                .withHostConfig(
                        (submission.getStageDefinition() instanceof StageWorkerDefinition swd
                         ? Optional.of(swd)
                         : Optional.<StageWorkerDefinition>empty())
                                .map(StageWorkerDefinition::requirements)
                                .map(this::hostConfigFromRequirements)
                                .orElseGet(HostConfig::new)
                                .withPrivileged(imageExt.isPrivileged())
                                .withShmSize(imageExt.getShmSizeMegabytes().map(Integer::longValue).orElse(null))
                                .withAutoRemove(true)
                                .withMounts(
                                        submission
                                                .getExtension(DockerVolumes.class)
                                                .map(this::mountsFromDockerVolumes)
                                                .orElseGet(Collections::emptyList)
                                )
                );


        return new SubmissionResult(stage, new DockerStageHandle(this.backend, stageId, createCmd));
    }

    @Nonnull
    private List<Mount> mountsFromDockerVolumes(@Nonnull DockerVolumes dockerVolumes) {
        return dockerVolumes
                .getVolumes()
                .stream()
                .flatMap(volume -> switch (volume.getType().toLowerCase()) {
                    case "nfs" -> Stream.of(createNfsMount(volume));
                    case "bind" -> Stream.of(createBindMount(volume));
                    default -> {
                        LOG.warning("Ignoring unexpected volume type '" + volume.getType() + "'");
                        yield Stream.empty();
                    }
                })
                .toList();
    }

    @Nonnull
    private Mount createNfsMount(@Nonnull DockerVolume volume) {
        return new Mount()
                .withTarget(volume.getContainerPath())
                .withReadOnly(volume.isReadonly())
                .withVolumeOptions(
                        new VolumeOptions()
                                .withDriverConfig(
                                        new Driver()
                                                .withName("local")
                                                .withOptions(
                                                        Map.of(
                                                                "type", "nfs",
                                                                "device", volume.getHostPath(),
                                                                "o", volume.getOptions()
                                                        )
                                                )
                                )
                );
    }

    @Nonnull
    private Mount createBindMount(@Nonnull DockerVolume volume) {
        return new Mount()
                .withType(MountType.BIND)
                .withSource(volume.getHostPath())
                .withTarget(volume.getContainerPath())
                .withReadOnly(volume.isReadonly());
    }

    @Nonnull
    @CheckReturnValue
    public HostConfig hostConfigFromRequirements(@Nonnull Requirements requirements) {
        var config = new HostConfig()
                .withCpuCount(requirements.getCpus() > 0 ? (long) requirements.getCpus() : null)
                .withMemory((long) requirements.getMegabytesOfRam());

        if (requirements.getGpu().getCount() > 0) {
            return config.withDeviceRequests(
                    List.of(
                            new DeviceRequest()
                                    .withDriver(requirements.getGpu().getVendor().orElse(DEFAULT_GPU_VENDOR))
                                    .withCapabilities(List.of(List.of("gpu")))
                                    .withCount(requirements.getGpu().getCount())
                    )
            );
        } else {
            return config;
        }
    }
}
