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

    public BaseRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        this.lockBus = lockBus;
        this.workDirectoryConfiguration = workDirectoryConfiguration;

        var dir = getRepositoryDirectory().toFile();
        if ((dir.exists() && !dir.isDirectory()) || (!dir.exists() && !dir.mkdirs())) {
            throw new IOException("Repository directory is not valid: " + dir);
        }
    }

    @Nonnull
    protected abstract Path getRepositoryDirectory();

    @Nonnull
    protected Path getRepositoryFile(String name) {
        return getRepositoryDirectory().resolve(Path.of(name).getFileName());
    }

    @Nonnull
    public Stream<Path> listAll() {
        return listAllInDirectory(getRepositoryDirectory());
    }

    @Nonnull
    protected Stream<Path> listAllInDirectory(Path directory) {
        try {
            return Files.list(directory);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list all entries in directory: " + directory, e);
            return Stream.empty();
        }
    }

    protected <T> Reader<T> defaultReader(Class<T> clazz) {
        return inputStream -> {
            try {
                return new Toml().read(inputStream).to(clazz);
            } catch (Throwable t) {
                throw new IOException("Failed to parse TOML", t);
            }
        };
    }

    protected <T> Writer<T> defaultWriter() {
        return (outputStream, value) -> new TomlWriter().write(value, outputStream);
    }

    protected <T> Optional<T> getUnsafe(Path path, Reader<T> reader) {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return Optional.of(reader.load(inputStream));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    protected <T> Optional<LockedContainer<T>> getLocked(Path path, Reader<T> reader, Writer<T> writer) {
        Lock lock = null;
        try {
            lock = getLockForPath(path);
            return Optional.of(new LockedContainer<>(lock, lockedReader(path, reader), lockedWriter(path, writer)));
        } catch (LockException | IOException e) {
            if (lock != null) {
                lock.release(); // only release on error, otherwise the returned value will hold the lock
            }
            return Optional.empty();
        }
    }

    private <T> LockedContainer.Writer<T> lockedWriter(Path path, Writer<T> writer) {
        return (l, value) -> {
            if (value != null) {
                try (OutputStream outputStream = new LockedOutputStream(path.toFile(), l)) {
                    writer.store(outputStream, value);
                }
            } else if (Files.isRegularFile(path)) {
                Files.delete(path);
            }
        };
    }

    private <T> LockedContainer.Reader<T> lockedReader(Path path, Reader<T> reader) {
        return lock -> {
            try (InputStream inputStream = new LockedInputStream(path.toFile(), lock)) {
                return reader.load(inputStream);
            } catch (FileNotFoundException e) {
                return null;
            }
        };
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
        @Nonnull private final Path      path;
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
