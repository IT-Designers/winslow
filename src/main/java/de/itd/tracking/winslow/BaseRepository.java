package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.fs.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class BaseRepository {

    public static final Logger LOG = Logger.getLogger(BaseRepository.class.getSimpleName());

    protected final LockBus                    lockBus;
    protected final WorkDirectoryConfiguration workDirectoryConfiguration;

    public BaseRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) {
        this.lockBus = lockBus;
        this.workDirectoryConfiguration = workDirectoryConfiguration;
    }

    @Nonnull
    public abstract Stream<Path> listAll();

    @Nonnull
    protected Stream<Path> listAllInDirectory(Path directory) {
        try {
            return Files.list(directory);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list all entries in directory: "+directory, e);
            return Stream.empty();
        }
    }

    protected <T> Reader<T> defaultReader(Class<T> clazz) {
        return inputStream -> new Toml().read(inputStream).to(clazz);
    }

    protected <T> Writer<T> defaultWriter() {
        return (outputStream, value) -> new TomlWriter().write(value, outputStream);
    }

    protected <T> Optional<T> getUnsafe(Path path, Reader<T> reader) {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return Optional.of(reader.load(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    protected <T> Optional<LockedContainer<T>> getLocked(Path path, Reader<T> reader, Writer<T> writer) {
        Lock lock = null;
        try {
            lock = getLockForPath(path);
            return Optional.of(
                    new LockedContainer<>(
                            lock,
                            l -> {
                                try (InputStream inputStream = new LockedInputStream(path.toFile(), l)) {
                                    return reader.load(inputStream);
                                } catch (FileNotFoundException e) {
                                    return null;
                                }
                            },
                            (l, value) -> {
                                try (OutputStream outputStream = new LockedOutputStream(path.toFile(), l)) {
                                    writer.store(outputStream, value);
                                }
                            }
                    )
            );
        } catch (LockException | IOException e) {
            if (lock != null) {
                lock.release();
            }
            return Optional.empty();
        }
    }

    protected Lock getLockForPath(Path path) throws LockException {
        var subject = workDirectoryConfiguration.getPath().relativize(path).toString();
        return new Lock(lockBus, subject);
    }

    protected interface Reader<T> {
        T load(InputStream inputStream) throws IOException;
    }

    protected interface Writer<T> {
        void store(OutputStream outputStream, T value) throws IOException;
    }

    protected <T> Handle<T> createHandle(@Nonnull Path path, @Nonnull Class<T> clazz) {
        return createHandle(path, defaultReader(clazz), defaultWriter());
    }

    protected <T> Handle<T> createHandle(@Nonnull Path path, @Nonnull Reader<T> reader, @Nonnull Writer<T> writer) {
        return new Handle<>(path, reader, writer);
    }

    public class Handle<T> {
        @Nonnull private final Path path;
        @Nonnull private final Reader<T> reader;
        @Nonnull private final Writer<T> writer;


        private Handle(@Nonnull Path path, @Nonnull Reader<T> reader, @Nonnull Writer<T> writer) {
            this.path = path;
            this.reader = reader;
            this.writer = writer;
        }

        @Nonnull
        public Optional<T> unsafe() {
            return BaseRepository.this.getUnsafe(path, reader);
        }

        @Nonnull
        public Optional<LockedContainer<T>> locked() {
            return BaseRepository.this.getLocked(path, reader, writer);
        }
    }
}
