package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.StatsInfo;

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

    /**
     * Extends the {@link State#Running} if there are logs that need to be collected first
     * @return The extended {@link State} of the {@link Gateway}
     */
    @Nonnull
    private State getGatewayState() {
        if (!this.gateway.logs.isEmpty()) {
            if (this.gateway.state == State.Preparing) {
                return State.Preparing;
            } else {
                return State.Running;
            }
        } else {
            return this.gateway.state;
        }
    }

    @Override
    public boolean isRunning() {
        return getGatewayState() == State.Running;
    }

    @Override
    public boolean hasStarted() {
        return getGatewayState() != State.Preparing;
    }

    @Override
    public boolean hasFinished() {
        return hasFailed() || hasSucceeded();
    }

    @Override
    public boolean hasFailed() {
        return getGatewayState() == State.Failed;
    }

    @Override
    public boolean hasSucceeded() {
        return getGatewayState() == State.Succeeded;
    }

    @Override
    public boolean isGone() {
        return false;
    }

    @Nonnull
    @Override
    public Optional<State> getState() {
        return Optional.of(getGatewayState());
    }

    @Nonnull
    @Override
    public Iterator<LogEntry> getLogs() throws IOException {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return !GatewayStageHandle.this.gateway.logs.isEmpty() || !hasStarted() || isRunning();
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
    public Optional<StatsInfo> getStats() throws IOException {
        return Optional.empty();
    }

    @Override
    public void stop() throws IOException {

    }

    @Override
    public void kill() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
