package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.Lock;
import de.itd.tracking.winslow.fs.LockException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockedContainer<T> implements Closeable {

    private static final Logger LOG = Logger.getLogger(LockedContainer.class.getSimpleName());

    @Nonnull private final Lock lock;
    @Nonnull private final Reader<T> reader;
    @Nonnull private final Writer<T> writer;

    private T value;

    public LockedContainer(
            @Nonnull Lock lock,
            @Nonnull Reader<T> reader,
            @Nonnull Writer<T> writer) throws IOException {
        this.lock = lock;
        this.reader = reader;
        this.writer = writer;

        this.value = this.reader.read(this.lock);
    }

    @Nonnull
    public Optional<T> get() throws LockException {
        this.lock.heartbeat();
        return Optional.ofNullable(this.value);
    }

    @Nonnull
    public Optional<T> getNoThrow() {
        try {
            return get();
        } catch (LockException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Nullable
    public T reload() throws IOException {
        this.value = this.reader.read(this.lock);
        return this.value;
    }

    public void update(@Nonnull T value) throws IOException {
        this.writer.write(this.lock, value);
        this.value = value;
    }

    public void delete() throws IOException {
        this.writer.write(this.lock, null);
        this.value = null;
    }

    public boolean deleteOmitExceptions() {
        try {
            this.delete();
            return true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to delete", e);
            return false;
        }
    }

    @Nonnull
    public Lock getLock() {
        return lock;
    }

    @Override
    public void close() {
        this.lock.close();
    }


    public interface Reader<T> {
        @Nullable
        T read(@Nonnull Lock lock) throws IOException;
    }

    public interface Writer<T> {
        void write(@Nonnull Lock lock, @Nullable T value) throws IOException;
    }

}
