package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.JobListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.*;
import de.itd.tracking.winslow.Backend;
import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.PreparedStageBuilder;
import de.itd.tracking.winslow.Stage;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
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

    public NomadBackend(@Nonnull NomadApiClient client) {
        this.client = client;
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

    @Nonnull
    @Override
    public Stream<LogEntry> getLogs(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        BlockingDeque<LogEntry> queue = new LinkedBlockingDeque<>();
        var                     weak  = new WeakReference<>(queue);

        Thread stdout = spawnStream(streamStdOut(pipeline, stage), weak, streamName(pipeline, stage, "stdout"));
        Thread stderr = spawnStream(streamStdErr(pipeline, stage), weak, streamName(pipeline, stage, "stderr"));
        Thread events = spawnStream(
                EventStream.stream(this, pipeline, stage),
                weak,
                streamName(pipeline, stage, "events")
        );


        Predicate<LogEntry> predicate = p -> {
            var weakGet = weak.get();
            return (weakGet != null && !weakGet.isEmpty()) || stdout.isAlive() || stderr.isAlive() || events.isAlive();
        };

        return Stream.iterate(
                null,
                predicate,
                prev -> {
                    while (predicate.test(null)) {
                        try {
                            synchronized (queue) {
                                var result = queue.poll(1, TimeUnit.SECONDS);
                                if (result != null) {
                                    return result;
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
        ).filter(Objects::nonNull);
    }

    @Nonnull
    @Override
    public PreparedStageBuilder newStageBuilder(
            @Nonnull String pipeline,
            @Nonnull String stage,
            @Nonnull StageDefinition stageDefinition) {
        return new NomadPreparedStageBuilder(pipeline, stage, getNewJobsApi(), stageDefinition);
    }

    private static String streamName(@Nonnull String pipeline, @Nonnull String stage, @Nonnull String stream) {
        return pipeline + "/" + stage + "." + stream;
    }

    private Stream<LogEntry> streamStdOut(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return LogStream.stdOut(
                getNewClientApi(),
                stage,
                () -> this.getAllocationListStubForOmitException(pipeline, stage)
        );
    }

    private Stream<LogEntry> streamStdErr(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return LogStream.stdOut(
                getNewClientApi(),
                stage,
                () -> this.getAllocationListStubForOmitException(pipeline, stage)
        );
    }

    private Thread spawnStream(
            @Nonnull Stream<LogEntry> source,
            @Nonnull WeakReference<BlockingDeque<LogEntry>> target,
            @Nonnull String name) {
        var thread = new Thread(() -> source
                .takeWhile(prev -> target.get() != null)
                .forEach(element -> {
                    var t = target.get();
                    if (t != null) {
                        t.add(element);
                    }
                }));
        thread.setDaemon(true);
        thread.setName(name);
        thread.start();
        return thread;
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
                .filter(alloc -> alloc.getTaskStates().get(stage) != null)
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
            var allocs = getAllocations();
            while (!predicate.test(allocs)) {
                try {
                    cachedAllocsSync.wait(CACHE_TIME_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return allocs;
        }
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
        return Optional.ofNullable(allocation.getTaskStates().get(taskName));
    }

    @Nonnull
    public static Stage.State toRunningStageState(@Nonnull TaskState task) {
        var failed   = hasTaskFailed(task);
        var started  = hasTaskStarted(task);
        var finished = hasTaskFinished(task);

        if (failed) {
            return Stage.State.Failed;
        } else if (!started || !finished) {
            return Stage.State.Running;
        } else {
            return Stage.State.Succeeded;
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
        return state.getFailed();
    }
}
