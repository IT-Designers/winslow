package de.itd.tracking.winslow.fs;

import javax.annotation.Nonnull;
import java.io.*;

public class LockedOutputStream extends OutputStream {

    @Nonnull private final OutputStream outputStream;
    @Nonnull private final Lock         lock;

    public LockedOutputStream(@Nonnull File file, @Nonnull Lock lock) throws FileNotFoundException {
        this(new FileOutputStream(file), lock);
    }

    public LockedOutputStream(@Nonnull OutputStream outputStream, @Nonnull Lock lock) {
        this.outputStream = outputStream;
        this.lock         = lock;
    }

    @Nonnull
    public Lock getLock() {
        return lock;
    }

    private void lockHeartbeat() throws IOException {
        try {
            this.lock.heartbeat();
        } catch (LockException e) {
            throw new IOException("Heartbeat on the lock failed", e);
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.lockHeartbeat();
        this.outputStream.write(i);
    }

    @Override
    public void write(@Nonnull byte[] b) throws IOException {
        this.lockHeartbeat();
        this.outputStream.write(b);
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
        this.lockHeartbeat();
        this.outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.lockHeartbeat();
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.lockHeartbeat();
        this.outputStream.close();
    }
}
