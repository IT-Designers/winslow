package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.JobListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.*;
import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class NomadBackend implements Backend {

    private static final long   CACHE_TIME_MS = 750;
    private static final Logger LOG           = Logger.getLogger(NomadBackend.class.getSimpleName());

    @Nonnull private final NomadApiClient client;

    private       long                     cachedAllocsTime;
    private       List<AllocationListStub> cachedAllocs;
    private final Object                   cachedAllocsSync = new Object();

    public NomadBackend(@Nonnull NomadApiClient client) throws IOException {
        this.client = client;
        killAnyRunningStage();
    }

    private void killAnyRunningStage() throws IOException {
        try {
            for (var alloc : this.client.getAllocationsApi().list().getValue()) {
                for (var task : alloc.getTaskStates().values()) {
                    if (!hasTaskFinished(task)) {
                        LOG.warning("Killing task that is running but was not started by this instance: " + alloc.getJobId());
                        getNewJobsApi().deregister(alloc.getJobId());
                    }
                }
            }
        } catch (NomadException e) {
            throw new IOException("Failed to communicate with nomad", e);
        }
    }



                /*
                var missing_devices = getClient()
                        .getEvaluationsApi()
                        .list()
                        .getValue()
                        .stream()
                        .filter(e -> stage.getJobId().equals(e.getJobId()))
                        .flatMap(e -> e.getFailedTgAllocs().values().stream())
                        .findFirst()
                        .stream()
                        .flatMap(tg -> tg.getConstraintFiltered().entrySet().stream())
                        .collect(Collectors.toUnmodifiableList());

                 */


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
        var allocations = getAllocations();
        return allocations
                .stream()
                .filter(alloc -> stage.equals(alloc.getJobId()))
                .findFirst()
                .flatMap(alloc -> getTask(alloc, stage))
                .map(NomadBackend::toRunningStageState);
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
                LogStream.stdOutIter(
                        getNewClientApi(),
                        stage,
                        () -> this.getAllocationListStubForOmitException(pipeline, stage)
                ),
                LogStream.stdErrIter(
                        getNewClientApi(),
                        stage,
                        () -> this.getAllocationListStubForOmitException(pipeline, stage)
                ),
                new EventStream(this, stage)
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
        if (this.cachedAllocs == null || this.cachedAllocsTime + CACHE_TIME_MS < System.currentTimeMillis()) {
            synchronized (cachedAllocsSync) {
                try {
                    this.cachedAllocs     = getNewAllocationsApi().list().getValue();
                    this.cachedAllocsTime = System.currentTimeMillis();
                    this.cachedAllocsSync.notifyAll();
                } catch (NomadException e) {
                    throw new IOException("Failed to list allocations", e);
                }
            }
        }
        return this.cachedAllocs;
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
    public Optional<TaskState> getTaskState(@Nonnull String stage) throws IOException {
        return getAllocations()
                .stream()
                .filter(alloc -> alloc.getJobId().equals(stage))
                .filter(alloc -> alloc.getTaskStates() != null)
                .flatMap(alloc -> Stream.ofNullable(alloc.getTaskStates().get(stage)))
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
        } else if (task.getState().contains("dead")) {
            return Stage.State.Failed;
        } else {
            return Stage.State.Running;
        }
    }

    @Nonnull
    public static Optional<Boolean> hasTaskStarted(AllocationListStub allocation, String taskName) {
        return getTask(allocation, taskName).map(NomadBackend::hasTaskStarted);
    }

    public static boolean hasTaskStarted(TaskState state) {
        return state.getStartedAt().after(new Date(1));
    }

    @Nonnull
    public static Optional<Boolean> hasTaskFinished(AllocationListStub allocation, String taskName) {
        return getTask(allocation, taskName).map(NomadBackend::hasTaskFinished);
    }

    public static boolean hasTaskFinished(TaskState state) {
        return state.getFinishedAt().after(new Date(1))
                || state.getState().toLowerCase().contains("dead")
                || hasTaskFailed(state);
    }

    @Nonnull
    public static Optional<Boolean> hasTaskFailed(AllocationListStub allocation, String taskName) {
        return getTask(allocation, taskName).map(NomadBackend::hasTaskFailed);
    }

    public static boolean hasTaskFailed(TaskState state) {
        return state.getFailed()
                || (!hasTaskStarted(state) && state.getState().toLowerCase().contains("dead"))
                || state.getEvents().stream().anyMatch(e -> e.getExitCode() != 0);
    }
}
