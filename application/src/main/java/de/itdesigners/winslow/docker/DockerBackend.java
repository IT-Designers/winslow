package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.*;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class DockerBackend implements Backend, Closeable, AutoCloseable {

    private static final Logger LOG                                  = Logger.getLogger(DockerBackend.class.getSimpleName());
    private final        String DOCKER_CONTAINER_NAME_WINSLOW_PREFIX = "winslow-stage-";

    private final @Nonnull DockerClient                       dockerClient;
    private final @Nonnull Map<String, StageHandle>           runningStages = new HashMap<>();
    private final @Nonnull SubmissionToDockerContainerAdapter adapter;

    public DockerBackend(@Nonnull DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        this.dockerClient.pingCmd().exec();

        this.adapter = new SubmissionToDockerContainerAdapter(this);

        recoverRunningStages();


        for (var container : this.dockerClient.listContainersCmd().exec()) {
            System.out.println(container);
        }


        var container = this.dockerClient
                .createContainerCmd("ubuntu")
                .withCmd("nvidia-smi")
                .withHostConfig(
                        HostConfig
                                .newHostConfig()
                                .withAutoRemove(true)
                                .withCpuCount(1L)
                                .withDeviceRequests(List.of(
                                        new DeviceRequest()
                                                .withDriver("nvidia")
                                                .withCapabilities(List.of(List.of("gpu")))
                                                .withCount(1)
                                ))
                                .withMounts(List.of(
                                        new Mount()
                                                .withTarget("/tmp/winslow")
                                                .withVolumeOptions(
                                                        new VolumeOptions()
                                                                .withDriverConfig(
                                                                        new Driver()
                                                                                .withName("local")
                                                                                .withOptions(
                                                                                        Map.of(
                                                                                                "type",
                                                                                                "nfs",
                                                                                                "device",
                                                                                                ":/data/streets/winslow",
                                                                                                "o",
                                                                                                "addr=10.202.6.22"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                ))
                )
                .exec();
        System.out.println(container.getId());
        System.out.println(Arrays.toString(container.getWarnings()));

        System.out.println(this.dockerClient.infoCmd().exec());

        this.dockerClient.startContainerCmd(container.getId()).exec();

        this.dockerClient
                .logContainerCmd(container.getId())
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ResultCallback.Adapter<>() {

                    @Override
                    public void onStart(Closeable stream) {
                        System.out.println("START");
                    }

                    @Override
                    public void onNext(Frame frame) {
                        System.out.println("RAW: " + frame.getRawValues());
                        System.out.println(frame);
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("COMPLETE");
                    }
                });
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
            // TODO
            System.out.println(getContainerName(container.getId()));
        }
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
        this.dockerClient.stopContainerCmd(getContainerName(stage)).exec();
    }

    @Override
    public void kill(@Nonnull String stageId) throws IOException {
        this.dockerClient.killContainerCmd(getContainerName(stageId)).exec();
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

        var gpuVendorRequirement = stage
                .getRequirements()
                .flatMap(Requirements::getGpu)
                .flatMap(Requirements.Gpu::getVendor);

        if (gpuVendorRequirement.isPresent()) {
            if (!hasContainerRuntime(info, gpuVendorRequirement.get())) {
                LOG.info("isCapableOfExecuting('" + stage.getName() + "') => false, runtime for '" + gpuVendorRequirement.get() + "' is missing");
                return false;
            }
        }

        return true;
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
