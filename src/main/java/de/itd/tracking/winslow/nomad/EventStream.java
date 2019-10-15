package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.TaskState;
import de.itd.tracking.winslow.LogEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Stream;

public class EventStream {

    public static final String MESSAGE_PREFIX = "[nomad] ";

    private final NomadBackend backend;
    private final String       stage;

    private TaskState       state         = null;
    private int             previousIndex = 0;
    private Queue<LogEntry> logs          = new ArrayDeque<>();

    public EventStream(NomadBackend backend, String stage) {
        this.backend = backend;
        this.stage   = stage;
    }

    private void maybeEnqueue(long time, boolean err, @Nonnull String message) {
        if (message.length() > 0 && (logs.isEmpty() || !message.equals(logs.peek().getMessage()))) {
            logs.add(new LogEntry(time, LogEntry.Source.MANAGEMENT_EVENT, err, MESSAGE_PREFIX + message));
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
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            if (this.state != null) {
                if (previousIndex < state.getEvents().size()) {
                    parseNextLogEntries();
                    return logs.poll();
                } else if (NomadBackend.hasTaskFinished(state)) {
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

    private void awaitNextEvent() throws IOException {
        backend.getTaskStatePollRepeatedlyUntil(this.stage, s -> s.map(state -> {
            boolean news = state.getEvents().size() > previousIndex || NomadBackend.hasTaskFinished(state);
            if (news) {
                this.state = state;
            }
            return news;
        }).orElse(Boolean.FALSE));
    }

    public static Stream<LogEntry> stream(@Nonnull NomadBackend backend, @Nonnull String stage) {
        var stream = new EventStream(backend, stage);
        return Stream
                .iterate(
                        new LogEntry(0, LogEntry.Source.MANAGEMENT_EVENT, false, ""),
                        Objects::nonNull,
                        v -> stream.next()
                )
                .skip(1);
    }
}
