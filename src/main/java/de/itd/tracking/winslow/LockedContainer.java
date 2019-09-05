package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.Lock;
import de.itd.tracking.winslow.fs.LockException;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

public class LockedContainer<T> implements Closeable {

    @Nonnull private final Lock lock;
    @Nonnull private final Reader<T> reader;
    @Nonnull private final Writer<T> writer;

    private T value;

    public LockedContainer(@Nonnull Lock lock, @Nonnull Reader<T> reader, @Nonnull Writer<T> writer) throws IOException {
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
    public T reload() throws IOException {
        this.value = this.reader.read(this.lock);
        return this.value;
    }

    public void update(@Nonnull T value) throws IOException {
        this.writer.write(this.lock, value);
        this.value = value;
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
        @Nonnull
        T read(@Nonnull Lock lock) throws IOException;
    }

    public interface Writer<T> {
        void write(@Nonnull Lock lock, @Nonnull T value) throws IOException;
    }

}
