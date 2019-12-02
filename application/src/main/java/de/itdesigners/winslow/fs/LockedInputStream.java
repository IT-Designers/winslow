package de.itdesigners.winslow.fs;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LockedInputStream extends InputStream {

    private final InputStream inputStream;
    private final Lock        lock;

    public LockedInputStream(File file, Lock lock) throws IOException {
        this(new FileInputStream(file), lock);
    }

    public LockedInputStream(InputStream fis, Lock lock) {
        this.inputStream = fis;
        this.lock        = lock;
    }

    private void lockHeartbeat() throws IOException {
        try {
            this.lock.heartbeat();
        } catch (LockException e) {
            throw new IOException("Heartbeat on the lock failed", e);
        }
    }

    @Override
    public int available() throws IOException {
        this.lockHeartbeat();
        return super.available();
    }

    @Override
    public void close() throws IOException {
        this.lockHeartbeat();
        this.inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        this.inputStream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return this.inputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        this.lockHeartbeat();
        return this.inputStream.read();
    }

    @Override
    public int read(@Nonnull byte[] b) throws IOException {
        this.lockHeartbeat();
        return this.inputStream.read(b);
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        this.lockHeartbeat();
        return this.inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        this.lockHeartbeat();
        return this.inputStream.skip(n);
    }
}
