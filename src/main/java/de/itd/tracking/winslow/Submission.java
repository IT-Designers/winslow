package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public interface Submission {

    enum State {
        Preparing,
        Running,
        Succeeded,
        Failed
    }

    @Nonnull
    State getState() throws OrchestratorConnectionException;

    @Nonnull
    default Optional<State> getStateOptional() {
        try {
            return Optional.of(getState());
        } catch (OrchestratorConnectionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    default Iterable<String> getStdOut() {
        return getStdOut(Integer.MAX_VALUE);
    }

    Iterable<String> getStdOut(int lastNLines);

    default Iterable<String> getStdErr() {
        return getStdErr(Integer.MAX_VALUE);
    }

    Iterable<String> getStdErr(int lastNLines);

    default boolean hasCompleted() throws OrchestratorConnectionException {
        return getState() == State.Failed || getState() == State.Succeeded;
    }

    default boolean hasCompletedSuccessfully() throws OrchestratorConnectionException {
        return getState() == State.Succeeded;
    }

    Stream<HistoryEntry> getHistory();

    interface HistoryEntry {
        long getTime();
        int getStageIndex();
        @Nonnull State getState();
        @Nonnull Optional<String> getDescription();

    }
}
