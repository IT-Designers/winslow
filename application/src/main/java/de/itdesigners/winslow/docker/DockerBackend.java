package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class DockerBackend implements Backend, Closeable, AutoCloseable {

    private static final Logger LOG                                  = Logger.getLogger(DockerBackend.class.getSimpleName());
    private static final String DOCKER_CONTAINER_NAME_WINSLOW_PREFIX = "winslow-stage-";

    private final @Nonnull String                             nodeName;
    private final @Nonnull DockerClient                       dockerClient;
    private final @Nonnull Map<String, StageHandle>           runningStages = new HashMap<>();
    private final @Nonnull SubmissionToDockerContainerAdapter adapter;

    public DockerBackend(@Nonnull String nodeName, @Nonnull DockerClient dockerClient) {
        this.nodeName     = nodeName;
        this.dockerClient = dockerClient;
        this.dockerClient.pingCmd().exec();

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
    @Override
    public Optional<State> getState(@Nonnull StageId stageId) throws IOException {
        return Optional.ofNullable(this.runningStages.get(stageId.getFullyQualified())).flatMap(StageHandle::getState);
    }

    @Override
    public void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        LOG.info(getContainerName(stage));
    }

    @Override
    public void stop(@Nonnull String stage) throws IOException {
        try {
            this.dockerClient.stopContainerCmd(getContainerName(stage)).exec();
        } catch (Throwable t) {
            throw new IOException(t);
        }
    }

    @Override
    public void kill(@Nonnull String stageId) throws IOException {
        try {
            this.dockerClient.killContainerCmd(getContainerName(stageId)).exec();
        } catch (Throwable t) {
            throw new IOException(t);
        }
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
    public SubmissionResult submit(@Nonnull Submission submission) throws IOException {
        try {
            return this.adapter.submit(submission);
        } catch (OrchestratorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        var info = this.dockerClient.infoCmd().exec();

        var gpuVendorRequirement = (stage instanceof StageWorkerDefinition swd
                                    ? Optional.of(swd)
                                    : Optional.<StageWorkerDefinition>empty())
                .map(StageWorkerDefinition::requirements)
                .map(Requirements::getGpu)
                .flatMap(Requirements.Gpu::getVendor);

        return gpuVendorRequirement
                .map(vendor -> {
                    if (!hasContainerRuntime(info, vendor)) {
                        LOG.info("isCapableOfExecuting('" + stage.name() + "') => false, runtime for '" + vendor + "' is missing");
                        return false;
                    } else {
                        return true;
                    }
                })
                .orElse(Boolean.TRUE);
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
