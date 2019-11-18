package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.*;
import de.itd.tracking.winslow.Backend;
import de.itd.tracking.winslow.CombinedIterator;
import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.config.Requirements;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.node.GpuInfo;
import de.itd.tracking.winslow.pipeline.PreparedStageBuilder;
import de.itd.tracking.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
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

    @Nonnull private final NomadApiClient client;

    private       long                     cachedAllocsTime;
    private       List<AllocationListStub> cachedAllocs;
    private final Object                   cachedAllocsSync = new Object();
    private       long                     cachedEvalsTime;
    private       List<Evaluation>         cachedEvals;
    private final Object                   cachedEvalsSync  = new Object();

    public NomadBackend(@Nonnull NomadApiClient client) throws IOException {
        this.client = client;
        killAnyRunningStage();


        /*

        try {
            getNewClient()
                    .getNodesApi()
                    .list()
                    .getValue()
                    .stream()
                    .forEach(stub -> {
                        try {
                            System.out.println(stub.getUnmappedProperties());
                            System.out.println("reserved " + getNewClient()
                                    .getNodesApi()
                                    .info(stub.getId())
                                    .getValue()
                                    .getReserved());
                            System.out.println("resources " + getNewClient()
                                    .getNodesApi()
                                    .info(stub.getId())
                                    .getValue());
                            System.out.println("unmapped " + ((List<Map<String, Object>>) ((Map<String, Object>) getNewClient()
                                    .getNodesApi()
                                    .info(stub.getId())
                                    .getValue()
                                    .getUnmappedProperties()
                                    .get("NodeResources"))
                                    .get("Devices"))
                                    .get(0)
                            );
                            System.out.println("attributes " + getNewClient()
                                    .getNodesApi()
                                    .info(stub.getId())
                                    .getValue()
                                    .getAttributes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NomadException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (NomadException e) {
            e.printStackTrace();
        }
         */
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
                                .getUnmappedProperties()
                )
                        .stream()
                        .flatMap(Stream::ofNullable)
                        .flatMap(map -> Stream.ofNullable((Map<String, Map<String, Object>>) map.get("NodeResources")))
                        .flatMap(map -> Stream.ofNullable((List<Map<String, Object>>) map.get("Devices")))
                        .flatMap(Collection::stream)
                        .filter(device -> "gpu".equals(device.get("Type")))
                        .flatMap(device -> {
                            var vendor = (String) device.get("Vendor");
                            var name   = (String) device.get("Name");
                            return ((List<Object>) device.get("Instances"))
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
    public Optional<Stage.State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
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
    public void kill(@Nonnull String stage) throws IOException {
        try {
            getNewJobsApi().deregister(stage).getValue();
        } catch (NomadException e) {
            throw new IOException("Failed to deregister job for " + stage, e);
        }
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return new CombinedIterator<>(
                LogStream.stdOutIter(getNewClientApi(), stage, this),
                LogStream.stdErrIter(getNewClientApi(), stage, this),
                new EventStream(this, stage),
                new EvaluationLogger(this, stage)
        );
    }

    @Nonnull
    @Override
    public PreparedStageBuilder newStageBuilder(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull StageDefinition stageDefinition) {
        return new NomadPreparedStageBuilder(pipeline, stage, getNewJobsApi(), stageDefinition);
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

    public Optional<AllocationListStub> getAllocationListStubForOmitException(
            @Nonnull String pipeline,
            @Nonnull String stage) {
        try {
            return getAllocationListStubFor(pipeline, stage);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load allocations for " + pipeline + " / " + stage, e);
            return Optional.empty();
        }
    }

    public Optional<AllocationListStub> getAllocationListStubFor(
            @Nonnull String pipeline,
            @Nonnull String stage) throws IOException {
        var allocs = getAllocations();
        return allocs
                .stream()
                .filter(alloc -> stage.equals(alloc.getJobId()))
                .filter(alloc -> alloc.getTaskStates() != null && alloc.getTaskStates().get(stage) != null)
                .findFirst();
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
    public List<AllocationListStub> getAllocationsPollRepeatedlyUntil(@Nonnull Predicate<List<AllocationListStub>> predicate) throws IOException {
        synchronized (this.cachedAllocsSync) {
            do {
                var allocs = getAllocations();
                if (!predicate.test(allocs)) {
                    try {
                        cachedAllocsSync.wait(CACHE_TIME_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    return allocs;
                }
            } while (true);
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
    public Optional<TaskState> getTaskStatePollRepeatedlyUntil(
            @Nonnull String stage,
            @Nonnull Predicate<Optional<TaskState>> predicate) throws IOException {
        return getAllocationsPollRepeatedlyUntil(allocs -> predicate
                .test(allocs
                              .stream()
                              .filter(alloc -> alloc.getJobId().equals(stage))
                              .filter(alloc -> alloc.getTaskStates() != null)
                              .flatMap(alloc -> Stream.ofNullable(alloc.getTaskStates().get(stage)))
                              .findFirst()
                ))
                .stream()
                .filter(alloc -> alloc.getJobId().equals(stage))
                .filter(alloc -> alloc.getTaskStates() != null)
                .flatMap(alloc -> Stream.ofNullable(alloc.getTaskStates().get(stage)))
                .findFirst();
    }

    @Nonnull
    public Optional<TaskState> getTaskState(@Nonnull String stage) throws IOException {
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
    private NomadApiClient getNewClient() {
        return new NomadApiClient(this.client.getConfig());
    }

    @Nonnull
    private ClientApi getNewClientApi() {
        return getNewClient().getClientApi(this.client.getConfig().getAddress());
    }

    @Nonnull
    private AllocationsApi getNewAllocationsApi() {
        return getNewClient().getAllocationsApi();
    }

    @Nonnull
    private JobsApi getNewJobsApi() {
        return getNewClient().getJobsApi();
    }


    @Nonnull
    public static Optional<TaskState> getTask(AllocationListStub allocation, String taskName) {
        return Optional.ofNullable(allocation.getTaskStates()).map(tasks -> tasks.get(taskName));
    }

    @Nonnull
    public static Stage.State toRunningStageState(@Nonnull TaskState task) {
        var failed   = hasTaskFailed(task);
        var started  = hasTaskStarted(task);
        var finished = hasTaskFinished(task);


        if (failed) {
            return Stage.State.Failed;
        } else if (started && finished) {
            return Stage.State.Succeeded;
        } else {
            return Stage.State.Running;
        }
    }

    public static boolean hasTaskStarted(TaskState state) {
        return state.getStartedAt().after(new Date(1));
    }

    public static boolean hasTaskFinished(TaskState state) {
        return state.getFinishedAt().after(new Date(1)) || state.getState().toLowerCase().contains("dead");
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
