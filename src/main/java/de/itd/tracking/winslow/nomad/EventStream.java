package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.TaskEvent;
import com.hashicorp.nomad.apimodel.TaskState;
import de.itd.tracking.winslow.LogEntry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class EventStream implements Iterator<LogEntry> {

    public static final String MESSAGE_PREFIX      = "[nomad] ";
    public static final int    LONG_TIME_THRESHOLD = 5_000;

    private final NomadBackend backend;
    private final String       stage;

    private TaskState       state                    = null;
    private int             previousIndex            = 0;
    private Deque<LogEntry> logs                     = new ArrayDeque<>();
    private boolean         terminatedEventProcessed = false;
    private Long            finishTime               = null;

    public EventStream(NomadBackend backend, String stage) {
        this.backend = backend;
        this.stage   = stage;
    }

    private void maybeEnqueue(long time, boolean err, @Nonnull String message) {
        if (message.length() > 0) {
            message = MESSAGE_PREFIX + message;
            if (logs.isEmpty() || !(message.equals(logs.peekLast().getMessage()) && time == logs
                    .peekLast()
                    .getTime())) {
                logs.add(new LogEntry(time, LogEntry.Source.MANAGEMENT_EVENT, err, message));
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!logs.isEmpty()) {
            return true;
        }
        if (this.hasFinishedLongTimeAgo()) {
            return false;
        }
        return !terminatedEventProcessed;
    }

    @Override
    public LogEntry next() {
        if (!this.logs.isEmpty()) {
            return this.logs.poll();
        }
        try {
            if (state == null) {
                backend.getTaskState(this.stage).map(this::stateUpdated);
            }
            if (state != null && NomadBackend.hasTaskFinished(state) && !terminatedEventProcessed) {
                backend.getTaskState(this.stage).map(this::stateUpdated);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tryParseNext().orElse(null);
    }

    private Optional<LogEntry> tryParseNext() {
        if (this.state != null) {
            while (previousIndex < state.getEvents().size()) {
                parseNextLogEntries();
            }
            this.state = null;
        }
        return Optional.ofNullable(this.logs.poll());
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

        this.terminatedEventProcessed |= isTerminated(next);
    }

    private boolean stateUpdated(@Nonnull TaskState state) {
        boolean news = state.getEvents().size() > previousIndex || NomadBackend.hasTaskFinished(state);
        if (news) {
            this.state = state;
        }
        if (this.finishTime == null && state.getFinishedAt() != null && state.getFinishedAt().after(new Date(1))) {
            this.finishTime = System.currentTimeMillis(); // use local timestamp as soon as finished has been detected
        }
        return news;
    }

    public static boolean isTerminated(@Nonnull TaskEvent event) {
        return isTerminated(event.getType());
    }

    public static boolean isTerminated(@Nonnull String type) {
        return "Terminated".equalsIgnoreCase(type);
    }

    private boolean hasFinishedLongTimeAgo() {
        return this.finishTime != null && System.currentTimeMillis() > this.finishTime + LONG_TIME_THRESHOLD;
    }
}
