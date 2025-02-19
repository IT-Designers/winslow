package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.model.*;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.StageHandle;
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
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .toList()
                )
                .withExposedPorts(
                        submission
                                .getExtension(DockerPortMappings.class)
                                .map(this::exposedPortsFromDockerMappings)
                                .orElseGet(Collections::emptyList)
                )
                .withHostConfig(
                        submission
                                .getHardwareRequirements()
                                .map(this::hostConfigFromRequirements)
                                .orElseGet(HostConfig::new)
                                .withPrivileged(imageExt.isPrivileged())
                                .withShmSize(imageExt.getShmSizeMegabytes().filter(m -> m > 0).orElse(null))
                                .withUlimits(imageExt.getShmSizeMegabytes().filter(m -> m > 0).map(m -> new Ulimit[]{
                                        new Ulimit("memlock", -1L, -1L)
                                }).orElse(null))
                                .withAutoRemove(true)
                                .withMounts(
                                        submission
                                                .getExtension(DockerVolumes.class)
                                                .map(this::mountsFromDockerVolumes)
                                                .orElseGet(Collections::emptyList)
                                )
                                .withPortBindings(
                                        submission
                                                .getExtension(DockerPortMappings.class)
                                                .map(this::portBindingsFromDockerMappings)
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
    private List<ExposedPort> exposedPortsFromDockerMappings(@Nonnull DockerPortMappings dockerPortMappings) {
        return dockerPortMappings
                .mappings()
                .stream()
                .map(mapping -> new ExposedPort(mapping.containerPort()))
                .toList();
    }

    @Nonnull
    private List<PortBinding> portBindingsFromDockerMappings(@Nonnull DockerPortMappings dockerPortMappings) {
        return dockerPortMappings
                .mappings()
                .stream()
                .map(mapping -> new PortBinding(
                        new Ports.Binding(mapping.hostInterfaceIp(), String.valueOf(mapping.hostPort())),
                        new ExposedPort(mapping.containerPort())
                ))
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
        var config = new HostConfig();

        requirements.getCpus().filter(cpus -> cpus > 0).ifPresent(cpus -> {
            config
                    .withCpuPeriod(100_000L)
                    .withCpuQuota(cpus * 100_000L);
        });

        config.withMemory(
                requirements
                        .getMegabytesOfRam()
                        .filter(mb -> mb < DockerBackend.DOCKER_MINIMUM_MEGABYTES_RAM)
                        .orElse(DockerBackend.DOCKER_MINIMUM_MEGABYTES_RAM)
                        * 1024 * 1024
        );

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
