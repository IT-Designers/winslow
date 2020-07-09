package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.*;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.api.node.GpuInfo;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.node.PlatformInfo;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NomadBackend implements Backend {

    private static final long   CACHE_TIME_MS                    = 750;
    private static final Logger LOG                              = Logger.getLogger(NomadBackend.class.getSimpleName());
    public static final  String DRIVER_ATTRIBUTE_DOCKER_RUNTIMES = "driver.docker.runtimes";
    public static final  String DEFAULT_GPU_VENDOR               = "nvidia";
    public static final  String IMAGE_DRIVER_NAME                = "docker";

    @Nonnull private final NomadApiClient              client;
    @Nonnull private final SubmissionToNomadJobAdapter submissionToNomadJobAdapter;

    private       long                     cachedAllocsTime;
    private       List<AllocationListStub> cachedAllocs;
    private final Object                   cachedAllocsSync = new Object();
    private       long                     cachedEvalsTime;
    private       List<Evaluation>         cachedEvals;
    private final Object                   cachedEvalsSync  = new Object();

    public NomadBackend(@Nonnull PlatformInfo platformInfo, @Nonnull NomadApiClient client) throws IOException {
        this.client                      = client;
        this.submissionToNomadJobAdapter = new SubmissionToNomadJobAdapter(platformInfo, this);
        killAnyRunningStage();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOG.info("Shutting down " + getClass().getSimpleName() + "...");
                this.killAnyRunningStage();
                LOG.info("Shutting down " + getClass().getSimpleName() + "... done");
            } catch (IOException e) {
                LOG.log(
                        Level.WARNING,
                        "Shutting down " + getClass().getSimpleName() + "... failed to kill at least one stage",
                        e
                );
            }
            System.out.flush();
            System.err.flush();
        }));
    }

    private void killAnyRunningStage() throws IOException {
        try {
            for (var alloc : this.client.getAllocationsApi().list().getValue()) {
                for (var task : alloc.getTaskStates().values()) {
                    if (!hasTaskFinished(task)) {
                        LOG.warning("Killing task that is running but was not started by this instance: " + alloc.getJobId());
                    } else {
                        LOG.info("Deleting job " + alloc.getJobId());
                    }
                    // remove it anyway
                    this.client.getJobsApi().deregister(alloc.getJobId()).getValue();
                }
            }
            LOG.info("Letting garbage be collected");
            this.client.getSystemApi().garbageCollect().getValue();
        } catch (NomadException e) {
            throw new IOException("Failed to communicate with nomad", e);
        }
    }

    private List<GpuInfo> listGpus() throws IOException {
        return listGpus(this.client);
    }

    @Nonnull
    protected static List<GpuInfo> listGpus(@Nonnull NomadApiClient client) throws IOException {
        try {
            var ids = client
                    .getNodesApi()
                    .list()
                    .getValue()
                    .stream()
                    .map(NodeListStub::getId)
                    .collect(Collectors.toList());

            var gpuInfo = new ArrayList<GpuInfo>();

            for (var id : ids) {
                Optional.ofNullable(
                        client
                                .getNodesApi()
                                .info(id)
                                .getValue()
                                .getNodeResources()
                )
                        .map(NodeResources::getDevices)
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(device -> "gpu".equals(device.getType()))
                        .flatMap(device -> {
                            var vendor = (String) device.getVendor();
                            var name   = (String) device.getName();
                            return device.getInstances()
                                    .stream()
                                    .map(instance -> new GpuInfo(vendor, name));
                        })
                        .forEach(gpuInfo::add);
            }

            return gpuInfo;

        } catch (NomadException e) {
            throw new IOException("Failed to contact Nomad instance", e);
        }
    }

    @Override
    @Nonnull
    public Stream<String> listStages() throws IOException {
        try {
            return getNewJobsApi()
                    .list()
                    .getValue()
                    .stream()
                    .map(JobListStub::getName);
        } catch (NomadException e) {
            throw new IOException("Failed to list jobs", e);
        }
    }

    @Nonnull
    @Override
    public Optional<State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return getTaskState(stage).map(NomadBackend::toRunningStageState);
    }

    @Override
    public void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        try {
            getNewJobsApi().deregister(stage).getValue();
        } catch (NomadException e) {
            throw new IOException("Failed to deregister job for " + pipeline + "/" + stage, e);
        }
    }

    @Override
    public void stop(@Nonnull String stage) throws IOException {
        try {
            var allocation = getAllocation(stage);
            if (allocation.isPresent()) {
                getNewAllocationsApi().signal(allocation.get().getId(), "SIGTERM", null);
            }
        } catch (NomadException e) {
            throw new IOException("Failed to signal allocation for " + stage, e);
        }
    }

    @Override
    public void kill(@Nonnull String stage) throws IOException {
        try {
            getNewJobsApi().deregister(stage).getValue();
        } catch (NomadException e) {
            throw new IOException("Failed to deregister job for " + stage, e);
        }
    }

    @Nonnull
    @Override
    public SubmissionResult submit(@Nonnull Submission submission) throws IOException {
        try {
            return submissionToNomadJobAdapter.submit(submission);
        } catch (OrchestratorException | NomadException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        try {
            return getNewClient()
                    .getNodesApi()
                    .list()
                    .getValue()
                    .stream()
                    .map(NodeListStub::getDrivers)
                    .flatMap(drivers -> drivers.entrySet().stream())
                    .filter(entry -> {
                        if (stage.getImage().isPresent()) {
                            return IMAGE_DRIVER_NAME.equalsIgnoreCase(entry.getKey());
                        } else {
                            return true;
                        }
                    })
                    .filter(entry -> {
                        var gpuRequired = stage.getRequirements().map(req -> req.getGpu().isPresent()).orElse(false);

                        var gpuVendor = stage
                                .getRequirements()
                                .flatMap(Requirements::getGpu)
                                .flatMap(Requirements.Gpu::getVendor);

                        var gpuAvailable = Optional
                                .ofNullable(entry.getValue().getAttributes().get(DRIVER_ATTRIBUTE_DOCKER_RUNTIMES))
                                .filter(runtimes -> runtimes.contains(gpuVendor.orElse(DEFAULT_GPU_VENDOR)))
                                .isPresent();

                        return !gpuRequired || gpuAvailable;
                    })
                    .anyMatch(entry -> entry.getValue().getHealthy() && entry.getValue().getDetected());
        } catch (IOException | NomadException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Nonnull
    public List<AllocationListStub> getAllocations() throws IOException {
        Supplier<Boolean> condition = () -> this.cachedAllocs == null || this.cachedAllocsTime + CACHE_TIME_MS < System.currentTimeMillis();

        // cheap check without lock
        if (condition.get()) {
            synchronized (cachedAllocsSync) {
                // check again to ensure it has not changed since acquiring the lock
                // this could be the case if some other thread had the same thought and already has loaded
                // the new allocations list
                if (condition.get()) {
                    try {
                        this.cachedAllocs     = getNewAllocationsApi().list().getValue();
                        this.cachedAllocsTime = System.currentTimeMillis();
                        this.cachedAllocsSync.notifyAll();
                    } catch (NomadException e) {
                        throw new IOException("Failed to list allocations", e);
                    }
                }
            }
        }

        return this.cachedAllocs;
    }

    @Nonnull
    public List<Evaluation> getEvaluations() throws IOException {
        synchronized (this.cachedEvalsSync) {
            if (this.cachedEvals == null || cachedEvalsTime + CACHE_TIME_MS < System.currentTimeMillis()) {
                try {
                    this.cachedEvals     = getNewClient().getEvaluationsApi().list().getValue();
                    this.cachedEvalsTime = System.currentTimeMillis();
                } catch (NomadException e) {
                    throw new IOException("Failed to update evaluations", e);
                }
            }
            return this.cachedEvals;
        }
    }

    @Nonnull
    public Optional<AllocationListStub> getAllocation(@Nonnull String stage) throws IOException {
        return getAllocations()
                .stream()
                .filter(alloc -> alloc.getJobId().equals(stage))
                .findFirst();
    }

    @Nonnull
    private Optional<TaskState> getTaskState(@Nonnull String stage) throws IOException {
        return getAllocation(stage)
                .stream()
                .flatMap(alloc -> Stream.ofNullable(alloc.getTaskStates()))
                .flatMap(states -> Stream.ofNullable(states.get(stage)))
                .findFirst()
                // append allocation failure state if allocation failed
                .or(() -> {
                        var state = hasAllocationFailed(stage)
                                .filter(e -> e)
                                .map(e -> allocationFailureTaskState());
                        if (state.isPresent()) {
                            LOG.info("Allocation failed for " + stage);
                        }
                        return state;
                    }
                );
    }

    private Optional<Boolean> hasAllocationFailed(@Nonnull String stage) {
        try {
            return Optional.of(allocationFailed(stage));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load allocations", e);
            return Optional.empty();
        }
    }

    private boolean allocationFailed(@Nonnull String stage) throws IOException {
        return getEvaluations(stage)
                .anyMatch(e -> (e.getFailedTgAllocs() != null) && "complete".equalsIgnoreCase(e.getStatus()));
    }

    @Nonnull
    public NomadApiClient getNewClient() {
        return new NomadApiClient(this.client.getConfig());
    }

    @Nonnull
    ClientApi getNewClientApi() {
        return getNewClient().getClientApi(this.client.getConfig().getAddress());
    }

    @Nonnull
    private AllocationsApi getNewAllocationsApi() {
        return getNewClient().getAllocationsApi();
    }

    @Nonnull
    protected JobsApi getNewJobsApi() {
        return getNewClient().getJobsApi();
    }


    @Nonnull
    public static State toRunningStageState(@Nonnull TaskState task) {
        var failed   = hasTaskFailed(task);
        var started  = hasTaskStarted(task);
        var finished = hasTaskFinished(task);


        if (failed) {
            return State.Failed;
        } else if (started && finished) {
            return State.Succeeded;
        } else {
            return State.Running;
        }
    }

    public static boolean hasTaskStarted(TaskState state) {
        return state.getStartedAt() != null && state.getStartedAt().after(new Date(1));
    }

    public static boolean hasTaskFinished(TaskState state) {
        return state.getFinishedAt() != null && (state
                .getFinishedAt()
                .after(new Date(1)) || (state.getState() != null && state.getState().toLowerCase().contains("dead")));
    }

    public static boolean hasTaskFailed(TaskState state) {
        return state.getFailed() || (
                hasTaskFinished(state) && state.getEvents().stream().anyMatch(e -> e.getExitCode() != 0)
        );
    }

    @Nonnull
    public Stream<Evaluation> getEvaluations(@Nonnull String stage) throws IOException {
        return getEvaluations()
                .stream()
                .filter(e -> stage.equals(e.getJobId()));
    }

    @Nonnull
    private static TaskState allocationFailureTaskState() {
        var state = new TaskState();
        state.setFailed(true);
        state.setEvents(Collections.emptyList());
        state.setStartedAt(new Date());
        state.setFinishedAt(new Date());
        state.setState("dead");
        return state;
    }
}
