package de.itdesigners.winslow;

import de.itdesigners.winslow.api.project.LogEntry;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.api.project.Stats;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class NoOpStageHandle implements StageHandle {

    private final boolean started;
    private final boolean succeeded;

    public NoOpStageHandle() {
        this(false, true);
    }

    public NoOpStageHandle(boolean started, boolean succeeded) {
        this.started   = started;
        this.succeeded = succeeded;
    }


    @Override
    public void pollNoThrows() {

    }

    @Override
    public void poll() throws IOException {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public boolean hasFinished() {
        return true;
    }

    @Override
    public boolean hasFailed() {
        return !hasSucceeded();
    }

    @Override
    public boolean hasSucceeded() {
        return succeeded;
    }

    @Override
    public boolean isGone() {
        return true;
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        if (hasSucceeded()) {
            return Optional.of(State.Succeeded);
        } else {
            return Optional.of(State.Failed);
        }
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return Collections.emptyIterator();
    }

    @Nonnull
    @Override
    public Optional<Stats> getStats() throws IOException {
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {

    }
}
