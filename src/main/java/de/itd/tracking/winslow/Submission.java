package de.itd.tracking.winslow;

import javax.annotation.Nonnull;

public interface Submission {

    enum State {
        Preparing,
        Running,
        Succeeded,
        Failed
    }

    @Nonnull
    State getState() throws OrchestratorConnectionException;

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
}
