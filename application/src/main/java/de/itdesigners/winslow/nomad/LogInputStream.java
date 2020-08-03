package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.StreamFrame;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.Backoff;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LogInputStream extends InputStream implements AutoCloseable {

    @Nonnull private final NomadApiClient   client;
    @Nonnull private final ClientApi        api;
    @Nonnull private final NomadStageHandle handle;
    @Nonnull private final String           logType;

    private long offset = 0;

    private FramedStream         framedStream;
    private ByteArrayInputStream currentFrame;
    private boolean              closed;

    public LogInputStream(
            @Nonnull NomadApiClient client,
            @Nonnull NomadStageHandle handle,
            @Nonnull String logType) throws IOException {
        this.client       = client;
        this.api          = client.getClientApi(client.getConfig().getAddress());
        this.handle       = handle;
        this.logType      = logType;
        this.framedStream = this.tryOpen();
    }

    @Nullable
    private FramedStream tryOpen() throws IOException {
        handle.pollNoThrows();
        if (!handle.hasFinished() && handle.getAllocationId().isPresent()) {
            try {
                return this.api.logsAsFrames(
                        handle.getAllocationId().get(),
                        handle.getFullyQualifiedStageId(),
                        false,
                        logType,
                        offset
                );
            } catch (NomadException ne) {
                // handle.notifyAboutPartialFailure(ne);
                // throw new IOException("NomadException while trying to access log stream: " + ne.getMessage(), ne);
                return null;
            } catch (IOException ioe) {
                handle.notifyAboutPartialFailure(ioe);
                throw ioe;
            } catch (Throwable t) {
                handle.notifyAboutPartialFailure(t);
                throw new IOException("Caught unexpected throwable: " + t.getMessage(), t);
            }
        } else {
            return null;
        }
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
        handle.pollNoThrows();
        return !handle.hasFinished();
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
                this.currentFrame = new ByteArrayInputStream(frame.getData());
                return true;
            } else {
                framedStream.close();
                framedStream = null;
            }
        } else {
            framedStream.close();
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
        if (this.ensureHasData()) {
            return currentFrame.available();
        } else if (!this.isAlive()) {
            return 100; // this will cause a read() which then will return -1 for EOF
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
            this.client.close();
        }
    }
}
