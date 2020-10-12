package de.itdesigners.winslow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.ExecutionGroupUpgrade;
import de.itdesigners.winslow.config.PipelineUpgrade;
import de.itdesigners.winslow.fs.*;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.web.RangedValueJsonModule;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseRepository {

    public static final String FILE_EXTENSION      = ".yml";
    public static final Logger LOG                 = Logger.getLogger(BaseRepository.class.getSimpleName());
    public static final int    SLEEP_BEFORE_RETRY  = 100;
    public static final int    DEFAULT_RETRY_COUNT = 1;

    protected final LockBus                    lockBus;
    protected final WorkDirectoryConfiguration workDirectoryConfiguration;

    public BaseRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        this.lockBus                    = lockBus;
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
    protected Path getRepositoryFile(String name, String suffix) {
        return getRepositoryFile(name + suffix);
    }

    @Nonnull
    protected Stream<Path> listAll(@Nonnull String suffix) {
        return listAllInDirectory(getRepositoryDirectory()).filter(path -> path.toString().endsWith(suffix));
    }

    @Nonnull
    protected static Stream<Path> listAllInDirectory(Path directory) {
        try (var files = Files.list(directory)) {
            return files.collect(Collectors.toUnmodifiableList()).stream();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list all entries in directory: " + directory, e);
            return Stream.empty();
        }
    }

    @Nonnull
    public static <T> Reader<T> defaultReader(Class<T> clazz) {
        return inputStream -> {
            try {
                var upgradeLoader = new SimpleModule();
                upgradeLoader.addDeserializer(ExecutionGroup.class, new ExecutionGroupUpgrade());
                upgradeLoader.addDeserializer(Pipeline.class, new PipelineUpgrade());

                return defaultObjectMapper()
                        .registerModule(new ParameterNamesModule())
                        .registerModule(upgradeLoader)
                        .readValue(inputStream, clazz);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to load " + clazz, t);
            }
        };
    }

    @Nonnull
    public static ObjectMapper defaultObjectMapper() {
        return defaultObjectMapperModules(new ObjectMapper(new YAMLFactory()).configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        ));
    }

    @Nonnull
    public static ObjectMapper defaultObjectMapperModules(@Nonnull ObjectMapper mapper) {
        return mapper
                .registerModule(new RangedValueJsonModule())
                .registerModule(new Jdk8Module());
    }

    public static <T> Writer<T> defaultWriter() {
        return (outputStream, value) -> defaultObjectMapper().writeValue(outputStream, value);
    }

    @Nonnull
    protected static <T> Optional<T> getUnsafe(Path path, Reader<T> reader) {
        for (int i = 0; i < 15; ++i) {
            try (InputStream inputStream = new FileInputStream(path.toFile())) {
                return Optional.of(reader.load(inputStream));
            } catch (IOException e) {
                if (!(e instanceof FileNotFoundException)) {
                    LOG.log(Level.SEVERE, "Failed to load file " + path, e);
                }
                LockBus.ensureSleepMs(i * 10);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    protected static Optional<String> getUnsafeString(Path path) {
        for (int i = 0; i < 15; ++i) {
            try {
                return Optional.ofNullable(Files.readString(path));
            } catch (IOException e) {
                if (!(e instanceof FileNotFoundException)) {
                    LOG.log(Level.SEVERE, "Failed to load file " + path, e);
                }
                LockBus.ensureSleepMs(i * 10);
            }
        }
        return Optional.empty();
    }

    protected boolean isLocked(@Nonnull Path path) {
        return this.lockBus.isLocked(getLockSubjectForPath(path));
    }

    protected boolean isLockedByAnotherInstance(@Nonnull Path path) {
        return this.lockBus.isLockedByAnotherInstance(getLockSubjectForPath(path));
    }

    protected <T> Optional<LockedContainer<T>> getLocked(Path path, Reader<T> reader, Writer<T> writer) {
        return getLocked(path, reader, writer, DEFAULT_RETRY_COUNT, e -> {
            if (e instanceof LockAlreadyExistsException) {
                LOG.log(Level.FINE, "Failed to lock " + path, e);
            } else {
                LOG.log(Level.WARNING, "Failed to lock" + path, e);
            }
        });
    }

    protected <T> Optional<LockedContainer<T>> getLocked(
            Path path,
            Reader<T> reader,
            Writer<T> writer,
            int retry,
            Consumer<LockException> logger) {
        for (int i = 0; i < Math.max(retry, 1); ++i) {
            try {
                return Optional.of(new LockedContainer<>(
                        getLockForPath(path),
                        lockedReader(path, reader),
                        lockedWriter(path, writer)
                ));
            } catch (LockException e) {
                logger.accept(e);
                if (i + 1 >= retry) {
                    return Optional.empty();
                } else {
                    LockBus.ensureSleepMs(SLEEP_BEFORE_RETRY);
                }
            }
        }
        // actually, this should be unreachable
        return Optional.empty();
    }

    private <T> LockedContainer.Writer<T> lockedWriter(Path path, Writer<T> writer) {
        return (l, value) -> {
            if (value != null) {
                var tmp = path.resolveSibling("." + path.getFileName().toString() + ".new");
                try (OutputStream outputStream = new LockedOutputStream(tmp.toFile(), l)) {
                    writer.store(outputStream, value);
                } catch (IOException e) {
                    Files.deleteIfExists(tmp);
                    throw e;
                }
                // move after the file has been closed and therefore after it has been flushed
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
        return getLockForPath(path, Lock.DEFAULT_LOCK_DURATION_MS);
    }

    protected Lock getLockForPath(Path path, int durationMs) throws LockException {
        String subject = getLockSubjectForPath(path);
        return new Lock(lockBus, subject, durationMs);
    }

    protected String getLockSubjectForPath(@Nonnull Path path) {
        return workDirectoryConfiguration.getPath().relativize(path).toString();
    }

    public interface Reader<T> {
        T load(InputStream inputStream) throws IOException;
    }

    public interface Writer<T> {
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
            this.path   = path;
            this.reader = reader;
            this.writer = writer;
        }

        /**
         * Unsafe meaning might fail to load and changes are not persisted.
         * Consider it as a shared read-only instance of the time it has been
         * loaded, if successful.
         *
         * @return A instance of the underlying at the time of the call or nothing on any error
         */
        @Nonnull
        public Optional<T> unsafe() {
            return BaseRepository.getUnsafe(path, reader);
        }

        @Nonnull
        public Optional<String> unsafeRaw() {
            return BaseRepository.getUnsafeString(path);
        }

        @Nonnull
        public Optional<LockedContainer<T>> exclusive() {
            return BaseRepository.this.getLocked(path, reader, writer);
        }

        public boolean isLocked() {
            return BaseRepository.this.isLocked(path);
        }

        public boolean isLockedByAnotherInstance() {
            return BaseRepository.this.isLockedByAnotherInstance(path);
        }

        public boolean exists() {
            return Files.exists(path);
        }
    }
}
