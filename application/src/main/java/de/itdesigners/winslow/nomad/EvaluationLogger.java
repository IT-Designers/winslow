package de.itdesigners.winslow.nomad;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.LogEntry;
import de.itdesigners.winslow.api.pipeline.LogSource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvaluationLogger implements Iterator<LogEntry> {

    private final @Nonnull NomadStageHandle handle;

    public EvaluationLogger(@Nonnull NomadStageHandle handle) {
        this.handle = handle;
    }

    @Override
    public boolean hasNext() {
        this.handle.pollNoThrows();
        return !handle.hasFinished();
    }

    @Override
    public LogEntry next() {
        try {
            List<String> result = handle
                    .getEvaluations()
                    .flatMap(e -> Stream.ofNullable(e.getFailedTgAllocs()))
                    .flatMap(e -> Stream.ofNullable(e.get(handle.getFullyQualifiedStageId())))
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
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private LogEntry kill(@Nonnull String message) {
        try {
            this.handle.pollNoThrows();
            this.handle.kill();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return new LogEntry(
                    System.currentTimeMillis(),
                    LogSource.MANAGEMENT_EVENT,
                    true,
                    Executor.PREFIX + message
            );
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
