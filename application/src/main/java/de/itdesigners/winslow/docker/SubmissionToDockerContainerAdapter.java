package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.model.*;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.pipeline.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SubmissionToDockerContainerAdapter {

    private static final Logger LOG = Logger.getLogger(SubmissionToDockerContainerAdapter.class.getSimpleName());

    private final @Nonnull DockerBackend backend;

    public SubmissionToDockerContainerAdapter(@Nonnull DockerBackend backend) {
        this.backend = backend;
    }

    @Nonnull
    public StageHandle submit(@Nonnull Submission submission) throws OrchestratorException, IOException {
        var stageId       = submission.getId().getFullyQualified();
        var containerName = backend.getContainerName(stageId);

        var imageExt = submission
                .getExtension(DockerImage.class)
                .orElseThrow(() -> new OrchestratorException("Missing DockerImage-Extension"));

        var createCmd = this.backend
                .getDockerClient()
                .createContainerCmd(imageExt.getImage())
                .withCmd(imageExt.getArguments())
                .withName(containerName)
                .withEnv(
                        submission
                                .getEffectiveEnvVariables()
                                .entrySet()
                                .stream()
                                .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                                .toList()
                )
                .withHostConfig(
                        submission
                                .getStageDefinition()
                                .getRequirements()
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


        return new DockerStageHandle(this.backend, stageId, createCmd);
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
                // .withReadOnly(volume.isReadonly())
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
                                                                        + (volume.isReadonly() ? ",ro" : "")
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
        // https://www.kernel.org/doc/Documentation/scheduler/sched-bwc.txt
        // https://github.com/moby/moby/issues/42356#issuecomment-833451641
        var config = new HostConfig()
                .withCpuPeriod(100_000L)
                .withCpuQuota(requirements.getCpu() > 0 ? (long) requirements.getCpu() * 100_000L : null)
                .withMemory(requirements.getMegabytesOfRam() * 1024 * 1024);

        return requirements
                .getGpu()
                .filter(g -> g.getCount() > 0 && g.getVendor().isPresent())
                .map(gpu -> config.withDeviceRequests(
                             List.of(
                                     new DeviceRequest()
                                             .withDriver(gpu.getVendor().get())
                                             .withCapabilities(List.of(List.of("gpu")))
                                             .withCount(gpu.getCount())
                             )
                     )
                ).orElse(config);
    }
}
