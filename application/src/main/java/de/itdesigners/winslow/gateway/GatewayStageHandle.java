package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.Stats;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

public class GatewayStageHandle implements StageHandle {

    private final @Nonnull Gateway gateway;
    private final @Nonnull Thread  thread;

    public GatewayStageHandle(@Nonnull Gateway gateway) {
        this.gateway = gateway;

        this.thread = new Thread(gateway);
        this.thread.setName("GateWay");
        this.thread.start();
    }

    @Override
    public void pollNoThrows() {
    }

    @Override
    public void poll() throws IOException {
    }

    @Override
    public boolean isRunning() {
        return this.gateway.state == State.Running;
    }

    @Override
    public boolean hasStarted() {
        return true;
    }

    @Override
    public boolean hasFinished() {
        return hasFailed() || hasSucceeded();
    }

    @Override
    public boolean hasFailed() {
        return this.gateway.state == State.Failed;
    }

    @Override
    public boolean hasSucceeded() {
        return this.gateway.state == State.Succeeded;
    }

    @Override
    public boolean isGone() {
        return false;
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        return Optional.of(this.gateway.state);
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return GatewayStageHandle.this.gateway.logs.isEmpty();
            }

            @Override
            public LogEntry next() {
                return GatewayStageHandle.this.gateway.logs.poll();
            }

            @Override
            public void remove() {
                // NOP
            }
        };
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
