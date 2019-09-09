package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;

public interface Stage {

    enum State {
        Running,
        Succeeded,
        Failed
    }

    @Nonnull
    StageDefinition getDefinition();

    @Nonnull
    Date getStartTime();

    @Nullable
    Date getFinishTime();

    @Nonnull
    State getState() throws OrchestratorConnectionException;

    @Nonnull
    default Optional<State> getStateOmitExceptions() {
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
}
