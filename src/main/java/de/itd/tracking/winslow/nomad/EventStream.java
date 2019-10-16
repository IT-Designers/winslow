package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.TaskState;
import de.itd.tracking.winslow.LogEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class EventStream implements Iterator<LogEntry> {

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
        if (message.length() > 0) {
            message = MESSAGE_PREFIX + message;
            if (logs.isEmpty() || !(message.equals(logs.peek().getMessage()) && time == logs.peek().getTime())) {
                logs.add(new LogEntry(time, LogEntry.Source.MANAGEMENT_EVENT, err, message));
            }
        }
    }

    @Override
    public boolean hasNext() {
        return !this.logs.isEmpty() || this.state == null || !NomadBackend.hasTaskFinished(this.state);
    }

    @Override
    public LogEntry next() {
        if (!this.logs.isEmpty()) {
            return this.logs.poll();
        }
        if (state == null) {
            try {
                if (backend.getTaskState(this.stage)
                           .map(this::stateUpdated)
                           .orElse(Boolean.FALSE)) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tryParseNext().orElse(null);
    }

    @Nullable
    private LogEntry nextNonNull() {
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
            var next = tryParseNext();
            if (next.isPresent()) {
                return next.get();
            }
        }
    }

    private Optional<LogEntry> tryParseNext() {
        if (this.state != null) {
            if (previousIndex < state.getEvents().size()) {
                parseNextLogEntries();
                return Optional.ofNullable(logs.poll());
            } else if (NomadBackend.hasTaskFinished(state)) {
                return Optional.empty();
            } else {
                this.state = null;
            }
        }
        return Optional.empty();
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
        backend.getTaskStatePollRepeatedlyUntil(this.stage, s -> s.map(this::stateUpdated).orElse(Boolean.FALSE));
    }

    private boolean stateUpdated(TaskState state) {
        boolean news = state.getEvents().size() > previousIndex || NomadBackend.hasTaskFinished(state);
        if (news) {
            this.state = state;
        }
        return news;
    }

    public static Stream<LogEntry> stream(@Nonnull NomadBackend backend, @Nonnull String stage) {
        var stream = new EventStream(backend, stage);
        return Stream
                .iterate(
                        new LogEntry(0, LogEntry.Source.MANAGEMENT_EVENT, false, ""),
                        Objects::nonNull,
                        v -> stream.nextNonNull()
                )
                .skip(1);
    }
}
