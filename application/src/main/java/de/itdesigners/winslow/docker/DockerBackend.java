package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Info;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.ResourceAllocationMonitor;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.node.PlatformInfo;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerBackend implements Backend, Closeable, AutoCloseable {

    private static final   Logger LOG                                  = Logger.getLogger(DockerBackend.class.getSimpleName());
    private static final   String DOCKER_CONTAINER_NAME_WINSLOW_PREFIX = "winslow-stage-";
    protected static final long   DOCKER_MINIMUM_MEGABYTES_RAM         = 10;

    private final @Nonnull String                             nodeName;
    private final @Nonnull DockerClient                       dockerClient;
    private final @Nonnull PlatformInfo                       platformInfo;
    private final @Nonnull SubmissionToDockerContainerAdapter adapter;

    private final @Nonnull Map<String, String> stageHandles = new HashMap<>();

    public DockerBackend(
            @Nonnull String nodeName,
            @Nonnull DockerClient dockerClient,
            @Nonnull PlatformInfo platformInfo) {
        this.nodeName     = nodeName;
        this.dockerClient = dockerClient;
        this.dockerClient.pingCmd().exec();
        this.platformInfo = platformInfo;

        this.adapter = new SubmissionToDockerContainerAdapter(this);

        recoverRunningStages();
    }

    private void recoverRunningStages() {
        var containers = this.dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec();

        var winslowContainers = containers
                .stream()
                .filter(c -> Arrays
                        .stream(c.getNames())
                        .anyMatch(this::isProbablyAWinslowStageContainer))
                .toList();

        for (var container : winslowContainers) {
            // TODO implement docker container recovery
            LOG.warning("Found container that could be recovered if recovery was implemented: " + container.getId());
        }
    }

    @Nonnull
    protected String getNodeName() {
        return nodeName;
    }

    @Nonnull
    protected DockerClient getDockerClient() {
        return dockerClient;
    }

    @Nonnull
    protected PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    @Nonnull
    protected String getContainerName(@Nonnull String stageId) {
        return DOCKER_CONTAINER_NAME_WINSLOW_PREFIX + stageId;
    }

    protected boolean isProbablyAWinslowStageContainer(@Nonnull String n) {
        return n.startsWith(DOCKER_CONTAINER_NAME_WINSLOW_PREFIX);
    }

    @Nonnull
    @Override
    public StageHandle submit(@Nonnull Submission submission) throws IOException {
        try {
            return this.adapter.submit(submission);
        } catch (OrchestratorException e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    @Override
    public ResourceAllocationMonitor.ResourceSet<Long> getRequiredResources(@Nonnull StageDefinition definition) {
        if (definition instanceof StageWorkerDefinition workerDefinition) {
            return new ResourceAllocationMonitor.ResourceSet<Long>()
                    .with(
                            ResourceAllocationMonitor.StandardResources.CPU,
                            (long) workerDefinition
                                    .requirements()
                                    .getCpus()
                                    .filter(cpus -> cpus > 0)
                                    .orElse(0)
                    )
                    .with(
                            ResourceAllocationMonitor.StandardResources.RAM,
                            workerDefinition
                                    .requirements()
                                    .getMegabytesOfRam()
                                    .filter(mb -> mb < DOCKER_MINIMUM_MEGABYTES_RAM)
                                    .orElse(DOCKER_MINIMUM_MEGABYTES_RAM)
                                    * 1024 * 1024

                    )
                    .with(
                            ResourceAllocationMonitor.StandardResources.GPU,
                            (long) workerDefinition.requirements().getGpu().getCount()

                    );

        } else {
            return new ResourceAllocationMonitor.ResourceSet<>();
        }
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stageDefinition) {
        return stageDefinition instanceof StageWorkerDefinition w
                && canFulfillHardwareRequirements(w.name(), w.requirements());
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull Submission submission) {
        return submission
                .getHardwareRequirements()
                .filter(requirements -> canFulfillHardwareRequirements(
                        submission.getId().getFullyQualified(),
                        requirements
                ))
                .isPresent();
    }

    private boolean canFulfillHardwareRequirements(@Nonnull String desc, @Nonnull Requirements requirements) {
        try {
            var info = this.dockerClient.infoCmd().exec();
            var noGpuRequired = requirements
                    .getGpu()
                    .getCount() == 0;
            var requiredGpuVendorPresent = requirements
                    .getGpu()
                    .getVendor()
                    .filter(vendor -> {
                        if (!hasContainerRuntime(info, vendor)) {
                            LOG.info("isCapableOfExecuting('" + desc + "') => false, runtime for '" + vendor + "' is missing");
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .isPresent();

            return noGpuRequired || requiredGpuVendorPresent;
        } catch (DockerException e) {
            LOG.log(Level.SEVERE, "Failed to retrieve docker info", e);
            return false;
        }
    }

    private boolean hasContainerRuntime(@Nonnull Info info, @Nonnull String runtime) {
        return Optional
                .ofNullable(info.getRawValues().get("Runtimes"))
                .flatMap(r -> {
                    if (r instanceof Map map) {
                        return Optional.of((Map<String, Object>) map);
                    } else {
                        return Optional.empty();
                    }
                })
                .filter(map -> map.containsKey(runtime))
                .isPresent();
    }

    @Override
    public void close() throws IOException {
        this.dockerClient.close();
    }
}
