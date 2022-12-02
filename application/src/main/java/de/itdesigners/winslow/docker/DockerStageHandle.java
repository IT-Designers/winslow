package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DockerStageHandle implements StageHandle {

    private final @Nonnull DockerBackend backend;
    private final @Nonnull String        containerId;

    private final @Nonnull Queue<LogEntry> logs = new ConcurrentLinkedQueue<>();

    private boolean running = true;
    private boolean gone    = false;

    public DockerStageHandle(@Nonnull DockerBackend backend, @Nonnull String containerId) {
        this.backend     = backend;
        this.containerId = containerId;

        this.backend
                .getDockerClient()
                .logContainerCmd(this.containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ResultCallback.Adapter<>() {

                    @Override
                    public void onStart(Closeable stream) {
                        // TODO
                    }

                    @Override
                    public void onNext(Frame frame) {
                        if (frame.getStreamType() == StreamType.STDERR || frame.getStreamType() == StreamType.STDOUT) {
                            DockerStageHandle.this.logs.add(new LogEntry(
                                    System.currentTimeMillis(),
                                    LogEntry.Source.STANDARD_IO,
                                    frame.getStreamType() == StreamType.STDERR,
                                    new String(frame.getPayload(), StandardCharsets.UTF_8).trim()
                            ));
                        }
                    }

                    @Override
                    public void onComplete() {
                        getResult();
                        cleanup();
                        DockerStageHandle.this.running.set(false);
                    }
                });
    }

    @Override
    public void pollNoThrows() {

    }

    @Override
    public void poll() throws IOException {

    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean hasStarted() {
        return true;
    }

    @Override
    public boolean hasFinished() {
        return !this.isRunning();
    }

    @Override
    public boolean hasFailed() {
        return false;
    }

    @Override
    public boolean hasSucceeded() {
        return false;
    }

    @Override
    public boolean isGone() {
        return this.gone;
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return null;
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
