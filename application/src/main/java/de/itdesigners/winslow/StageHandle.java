package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

public interface StageHandle extends AutoCloseable {

    /**
     * Handles errors internally, might cause states to be
     * updated accordingly (like setting a failed flag)
     * <p>
     * Polls for updates for this handle
     */
    void pollNoThrows();

    /**
     * Polls for updates for this handle
     */
    void poll() throws IOException;

    boolean isRunning();

    /**
     * @return Whether the corresponding stage has started at some point in time
     */
    boolean hasStarted();

    /**
     * @return Whether the corresponding stage has finished executing. Might include a cool down propagation delay.
     */
    boolean hasFinished();

    boolean hasFailed();

    boolean hasSucceeded();

    /**
     * @return Whether the {@link StageHandle} points to an invalid stage execution,
     * for example, once the stage has been cleaned up by the {@link Backend}
     */
    boolean isGone();

    @Nonnull
    Optional<State> getState();

    @Nonnull
    Iterator<LogEntry> getLogs() throws IOException;

    @Nonnull
    Optional<Stats> getStats() throws IOException;

    /**
     * Issues a request to stop the execution of this {@link StageHandle}.
     * @throws IOException If the request could not be processed.
     */
    void stop() throws IOException;

    /**
     * Issues a request to stop the execution of this {@link StageHandle} immediately.
     * @throws IOException If the request could not be processed.
     */
    void kill() throws IOException;

    @Override
    void close() throws IOException;
}
