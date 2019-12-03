package de.itdesigners.winslow.nomad;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.api.project.LogEntry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvaluationLogger implements Iterator<LogEntry> {

    private static final long START_TIMEOUT_MS = 30_000;

    private final @Nonnull NomadBackend backend;
    private final @Nonnull String       stage;
    private final          long         startTimeMs;

    private boolean killSubmitted = false;

    public EvaluationLogger(@Nonnull NomadBackend backend, @Nonnull String stage) {
        this.backend     = backend;
        this.stage       = stage;
        this.startTimeMs = System.currentTimeMillis();
    }

    @Override
    public boolean hasNext() {
        try {
            // interesting states are
            // - not started yet (empty)
            // - failed but not processed yet (potential error to log)
            return backend.getTaskState(this.stage)
                          .map(s -> (startTimeoutReached() || s.getFailed()) ^ killSubmitted)
                          .orElse(Boolean.TRUE);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public LogEntry next() {
        try {
            List<String> result = backend
                    .getEvaluations(stage)
                    .flatMap(e -> Stream.ofNullable(e.getFailedTgAllocs()))
                    .flatMap(e -> Stream.ofNullable(e.get(stage)))
                    .flatMap(e -> {
                        Stream<String> constraint = e.getConstraintFiltered() != null
                                                    ? e.getConstraintFiltered().keySet().stream()
                                                    : Stream.empty();
                        Stream<String> dimensions = e.getDimensionExhausted() != null
                                                    ? e.getDimensionExhausted().keySet().stream()
                                                    : Stream.empty();

                        return Stream.concat(constraint, dimensions);
                    })
                    .collect(Collectors.toList());

            if (!result.isEmpty()) {
                return kill(
                        "Failed to start because of exhausted resource"
                                + (result.size() > 1 ? "s" : "")
                                + ": "
                                + String.join(", ", result)
                );
            } else if (startTimeoutReached()) {
                return kill("Start timeout reached");
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private LogEntry kill(@Nonnull String message) {
        try {
            this.killSubmitted = true;
            backend.kill(stage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return new LogEntry(
                    System.currentTimeMillis(),
                    LogEntry.Source.MANAGEMENT_EVENT,
                    true,
                    Executor.PREFIX + message
            );
        }
    }

    private boolean startTimeoutReached() {
        boolean timeout = (startTimeMs + START_TIMEOUT_MS < System.currentTimeMillis());
        try {
            return backend.getTaskState(this.stage).isEmpty() && timeout;
        } catch (IOException e) {
            e.printStackTrace();
            return timeout;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{"
                + "hashNext()=" + hasNext()
                + "}#"
                + hashCode();
    }
}
