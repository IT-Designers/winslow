package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.StreamFrame;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Backoff;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

public class LogInputStream extends InputStream implements AutoCloseable {

    @Nonnull private final ClientApi                              api;
    @Nonnull private final String                                 taskName;
    @Nonnull private final Supplier<Optional<AllocationListStub>> stateSupplier;
    @Nonnull private final String                                 logType;
    private final          boolean                                follow;

    private long   offset = 0;
    private String file   = null;

    private FramedStream         framedStream;
    private ByteArrayInputStream currentFrame;
    private boolean              closed;
    private long                 lastSuccess = 0;

    public LogInputStream(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier,
            @Nonnull String logType,
            boolean follow) throws IOException {
        this.api           = api;
        this.taskName      = taskName;
        this.stateSupplier = stateSupplier;
        this.logType       = logType;
        this.follow        = follow;
        this.framedStream  = this.tryOpen();
    }

    @Nullable
    private FramedStream tryOpen() throws IOException {
        var allocationBeingPresentOnlyIfHasStarted = getAllocationBeingPresentOnlyIfHasStarted();
        if (allocationBeingPresentOnlyIfHasStarted.isPresent()) {
            try {
                if (file == null) {
                    return api.logsAsFrames(
                            allocationBeingPresentOnlyIfHasStarted.get().getId(),
                            taskName,
                            false,
                            logType
                    );
                } else if (follow) {
                    return api.stream(allocationBeingPresentOnlyIfHasStarted.get().getId(), file, offset);
                } else {
                    closed = true;
                    return null;
                }
            } catch (NomadException e) {
                throw new IOException("NomadException while trying to access log", e);
            }
        } else {
            return null;
        }
    }

    private Optional<AllocationListStub> getAllocationBeingPresentOnlyIfHasStarted() {
        return this.stateSupplier.get().filter(allocation -> NomadOrchestrator
                .hasTaskStarted(allocation, taskName)
                .orElse(Boolean.FALSE));
    }

    private boolean tryOpenIfNotOpened() throws IOException {
        if (this.framedStream != null || this.closed) {
            return false;
        } else {
            this.framedStream = tryOpen();
            return this.framedStream != null;
        }
    }

    private boolean isAlive() {
        return this.stateSupplier
                .get()
                .flatMap(allocation -> NomadOrchestrator.hasTaskFinished(allocation, taskName))
                .map(v -> {
                    if (v) {
                        return Boolean.FALSE;
                    } else {
                        lastSuccess = System.currentTimeMillis();
                        return Boolean.TRUE;
                    }
                })
                .orElse(Boolean.TRUE) || (System.currentTimeMillis() - lastSuccess) < 5_000;
    }

    private boolean hasCompleted() {
        var alloc = getAllocationBeingPresentOnlyIfHasStarted();
        return alloc
                .flatMap(allocation -> NomadOrchestrator.hasTaskFinished(allocation, taskName))
                .orElse(Boolean.FALSE);
    }

    interface CallableIOException {
        /**
         * @return >=-1 for data, =< -2 for WouldBlockInfinitely
         * @throws IOException
         */
        int call() throws IOException;
    }

    private int polled(@Nonnull CallableIOException callable) throws IOException {
        var backoff = new Backoff(50, 1_000, 1.5f);
        for (int i = 0; i == 0 || isAlive(); ++i) {
            var value = callable.call();
            if (value >= -1) {
                return value;
            } else {
                backoff.sleep();
            }
        }
        return -1;
    }

    private boolean ensureHasData() throws IOException {
        if (this.currentFrame != null) {
            if (this.currentFrame.available() > 0) {
                this.lastSuccess = System.currentTimeMillis();
                return true;
            } else {
                this.currentFrame.close();
                this.currentFrame = null;
            }
        }

        if (framedStream == null && !tryOpenIfNotOpened()) {
            return false;
        }

        if (framedStream.hasNextFrame()) {
            StreamFrame frame = framedStream.nextFrame();
            if (frame != null && frame.getData() != null && frame.getData().length > 0) {
                this.offset += frame.getData().length;
                this.file         = frame.getFile();
                this.currentFrame = new ByteArrayInputStream(frame.getData());
                this.lastSuccess  = System.currentTimeMillis();
                return true;
            }
        } else {
            framedStream = null;
        }
        return false;
    }

    @Override
    public int read() throws IOException {
        return polled(() -> {
            if (ensureHasData()) {
                return this.currentFrame.read();
            }
            return -2;
        });
    }

    @Override
    public int read(@Nonnull byte[] bytes) throws IOException {
        return this.read(bytes, 0, bytes.length);
    }

    @Override
    public int read(@Nonnull byte[] bytes, int off, int len) throws IOException {
        return polled(() -> {
            if (ensureHasData()) {
                return this.currentFrame.read(bytes, off, len);
            }
            return -2;
        });
    }

    @Override
    public int available() throws IOException {
        if (this.currentFrame != null) {
            return currentFrame.available();
        } else {
            return 0;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.framedStream != null) {
                this.framedStream.close();
            }
        } finally {
            this.closed       = true;
            this.framedStream = null;
        }
    }
}
