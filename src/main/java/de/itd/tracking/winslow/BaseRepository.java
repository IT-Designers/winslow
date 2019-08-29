package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.fs.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class BaseRepository {
    public static final Logger LOG = Logger.getLogger(BaseRepository.class.getSimpleName());

    protected final LockBus                    lockBus;
    protected final WorkDirectoryConfiguration workDirectoryConfiguration;

    public BaseRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) {
        this.lockBus = lockBus;
        this.workDirectoryConfiguration = workDirectoryConfiguration;
    }

    protected <T> Loader<T> defaultLoader(Class<T> clazz) {
        return inputStream -> new Toml().read(inputStream).to(clazz);
    }

    protected <T> Stream<T> getAllInDirectoryUnsafe(Path path, Class<T> clazz) {
        return getAllInDirectoryUnsafe(path, clazz, defaultLoader(clazz));
    }

    protected <T> Stream<T> getAllInDirectoryUnsafe(Path path, Class<T> clazz, Loader<T> loader) {
        try {
            return Files
                    .list(path)
                    .flatMap(p -> {
                        var subject = workDirectoryConfiguration.getPath().relativize(p.toAbsolutePath()).toString();
                        try (InputStream inputStream = new FileInputStream(p.toFile())) {
                            return Stream.of(loader.load(inputStream));
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, "Failed to load " + clazz.getSimpleName(), e);
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list " + clazz.getSimpleName() + "s", e);
            return Stream.empty();
        }
    }

    protected <T> Stream<T> getAllInDirectory(Path path, Class<T> clazz) {
        return getAllInDirectory(path, clazz, defaultLoader(clazz));
    }

    protected <T> Stream<T> getAllInDirectory(Path path, Class<T> clazz, Loader<T> loader) {
        try {
            return Files
                    .list(path)
                    .flatMap(p -> {
                        var subject = workDirectoryConfiguration.getPath().relativize(p.toAbsolutePath()).toString();
                        try (Lock lock = new Lock(lockBus, subject)) {
                            try (InputStream inputStream = new LockedInputStream(p.toFile(), lock)) {
                                return Stream.of(loader.load(inputStream));
                            }
                        } catch (LockException | IOException e) {
                            LOG.log(Level.SEVERE, "Failed to load " + clazz.getSimpleName(), e);
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list " + clazz.getSimpleName() + "s", e);
            return Stream.empty();
        }
    }

    protected <T> Optional<T> getSingleUnsafe(Path path, Class<T> clazz) {
        return getSingleUnsafe(path, clazz, defaultLoader(clazz));
    }

    protected <T> Optional<T> getSingleUnsafe(Path path, Class<T> clazz, Loader<T> loader) {
        var subject = workDirectoryConfiguration.getPath().relativize(path).toString();
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return Optional.of(loader.load(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    protected <T> Optional<T> getSingle(Path path, Class<T> clazz) {
        return getSingle(path, clazz, defaultLoader(clazz));
    }

    protected <T> Optional<T> getSingle(Path path, Class<T> clazz, Loader<T> loader) {
        var subject = workDirectoryConfiguration.getPath().relativize(path).toString();
        try (Lock lock = new Lock(lockBus, subject)) {
            try (InputStream inputStream = new LockedInputStream(path.toFile(), lock)) {
                return Optional.of(loader.load(inputStream));
            }
        } catch (LockException | IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    protected interface Loader<T> {
        T load(InputStream inputStream) throws IOException;
    }
}
