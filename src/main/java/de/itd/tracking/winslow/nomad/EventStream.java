package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.TaskState;
import com.hashicorp.nomad.javasdk.AllocationsApi;
import com.hashicorp.nomad.javasdk.NomadException;
import com.hashicorp.nomad.javasdk.QueryOptions;
import com.hashicorp.nomad.javasdk.WaitStrategy;
import de.itd.tracking.winslow.LogEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Stream;

public class EventStream {

    private final AllocationsApi api;
    private final String         jobId;
    private final String         taskName;

    private TaskState       state = null;
    private int             previousIndex = 0;
    private Queue<LogEntry> logs = new ArrayDeque<>();

    public EventStream(AllocationsApi api, String jobId, String taskName) {
        this.api = api;
        this.jobId = jobId;
        this.taskName = taskName;
    }

    @Nullable
    private TaskState getTaskState(@Nonnull List<AllocationListStub> list) {
        for (AllocationListStub alloc : list) {
            if (this.jobId.equals(alloc.getJobId()) && alloc.getTaskStates() != null && alloc
                    .getTaskStates()
                    .get(this.taskName) != null) {
                return alloc.getTaskStates().get(this.taskName);
            }
        }
        return null;
    }

    private void maybeEnqueue(long time, boolean err, @Nonnull String message) {
        if (message.length() > 0 && (logs.isEmpty() || !message.equals(logs.peek().getMessage()))) {
            logs.add(new LogEntry(time, LogEntry.Source.MANAGEMENT_EVENT, err, message));
        }
    }

    @Nullable
    private LogEntry next() {
        while (true) {
            if (!this.logs.isEmpty()) {
                return this.logs.poll();
            }
            if (state == null) {
                try {
                    awaitNextEvent();
                } catch (IOException | NomadException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            if (this.state != null) {
                if (previousIndex < state.getEvents().size()) {
                    parseNextLogEntries();
                    return logs.poll();
                } else if (state.getFinishedAt() != null && state.getFinishedAt().getTime() > 0) {
                    return null;
                } else {
                    this.state = null;
                }
            }
        }
    }

    private void parseNextLogEntries() {
        var next = state.getEvents().get(previousIndex++);
        var time = next.getTime() / 1_000_000;
        boolean err = next.getFailsTask() || next.getDownloadError().length() > 0 || next
                .getDriverError()
                .length() > 0 || next.getKillError().length() > 0 || next.getSetupError().length() > 0 || next
                .getValidationError()
                .length() > 0 || next.getVaultError().length() > 0;

        maybeEnqueue(time, err, next.getMessage());
        maybeEnqueue(time, err, next.getDisplayMessage());
        maybeEnqueue(time, err, next.getDriverMessage());

        maybeEnqueue(time, err, next.getDownloadError());
        maybeEnqueue(time, err, next.getDriverError());
        maybeEnqueue(time, err, next.getKillError());
        maybeEnqueue(time, err, next.getSetupError());
        maybeEnqueue(time, err, next.getValidationError());
        maybeEnqueue(time, err, next.getVaultError());
    }

    private void awaitNextEvent() throws IOException, NomadException {
        api.list(QueryOptions.pollRepeatedlyUntil(response -> {
            var state = getTaskState(response.getValue());
            if (state == null) {
                return false;
            } else {
                boolean news = state.getEvents().size() > previousIndex || (state.getFinishedAt() != null && state
                        .getFinishedAt()
                        .getTime() > 0);
                if (news) {
                    this.state = state;
                }
                return news;
            }
        }, WaitStrategy.WAIT_INDEFINITELY));
    }

    public static Stream<LogEntry> stream(
            @Nonnull AllocationsApi api,
            @Nonnull String jobId,
            @Nonnull String taskName) {
        var stream = new EventStream(api, jobId, taskName);
        return Stream.iterate(new LogEntry(0, LogEntry.Source.MANAGEMENT_EVENT, false, ""),
                              Objects::nonNull,
                              v -> stream.next()
                             ).skip(1);
    }
}
