package de.itdesigners.winslow.nomad;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.LogEntry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvaluationLogger implements Iterator<LogEntry> {

    @Nonnull private final NomadBackend backend;
    @Nonnull private final String       stage;

    private boolean killSubmitted = false;

    public EvaluationLogger(@Nonnull NomadBackend backend, @Nonnull String stage) {
        this.backend = backend;
        this.stage   = stage;
    }

    @Override
    public boolean hasNext() {
        try {
            // interesting states are
            // - not started yet (empty)
            // - failed but not processed yet (potential error to log)
            return backend.getTaskState(this.stage)
                          .map(s -> s.getFailed() ^ killSubmitted)
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
                            Executor.PREFIX
                                    + "Failed to start because of exhausted resource"
                                    + (result.size() > 1 ? "s" : "")
                                    + ": "
                                    + String.join(", ", result)
                    );
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }
}
