package de.itdesigners.winslow;

import de.itdesigners.winslow.auth.NameNotFoundException;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

public abstract class CachedBaseRepository<T> extends BaseRepository {

    protected final @Nonnull CachedFunction<String, Optional<T>> cachedLoader;

    public CachedBaseRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        this.cachedLoader = new CachedFunction<>(name -> getHandle(name).unsafe());

        lockBus.registerEventListener(
                Event.Command.RELEASE,
                event -> {
                    var eventPath = Path.of(event.getSubject()).normalize();
                    var eventFile = eventPath.getFileName().toString();
                    var dir       = getRepositoryDirectory();
                    var parent    = workDirectoryConfiguration.getPath().resolve(eventPath).getParent().toUri();

                    if (parent.getPath().equals(dir.toUri().getPath()) && eventFile.endsWith(FILE_EXTENSION)) {
                        var name = eventFile.substring(0, eventFile.length() - FILE_EXTENSION.length());
                        synchronizedVoidLoader(loader -> loader.forget(name));
                    }
                },
                LockBus.RegistrationOption.NOTIFY_ONLY_IF_ISSUER_IS_NOT_US
        );
    }

    @Nonnull
    protected abstract Class<T> getValueType();

    @Nonnull
    protected Handle<T> getHandle(@Nonnull String name) {
        return createHandle(getFilePath(name), getValueType());
    }

    @Nonnull
    protected Path getFilePath(@Nonnull String name) {
        return getRepositoryDirectory().resolve(Path.of(name + FILE_EXTENSION).normalize().getFileName());
    }

    protected synchronized <O> O synchronizedLoader(@Nonnull Function<CachedFunction<String, Optional<T>>, O> callback) {
        return callback.apply(this.cachedLoader);
    }

    protected void synchronizedVoidLoader(Consumer<CachedFunction<String, Optional<T>>> callback) {
        this.synchronizedLoader(loader -> {
            callback.accept(loader);
            return null;
        });
    }

    @Nonnull
    protected Optional<T> loadUnsafeNoThrows(@Nonnull String name) {
        return synchronizedLoader(loader -> loader.apply(name));
    }

    /**
     * @return IO-Stream that must be called {@link Stream#close()} on manually!
     */
    @Nonnull
    protected Stream<String> listFileNamesWithoutFileExtensionNoThrows() {
        Function<Stream<Path>, Stream<String>> noneThrowingMapping = stream -> stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(FILE_EXTENSION))
                .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                .toList()
                .stream();

        try {
            return noneThrowingMapping.apply(Files.list(getRepositoryDirectory()));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list directory", e);
            return Stream.empty();
        }
    }

    protected boolean storeIfNotExists(@Nonnull String name, @Nonnull T value) throws IOException {
        if (loadUnsafeNoThrows(name).isEmpty()) {
            try (var container = getHandle(name).exclusive().orElseThrow()) {
                container.update(value);
            }
            synchronizedVoidLoader(loader -> loader.remember(name, Optional.of(value)));
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    protected <E extends Throwable> T updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<T, T, E> updater) throws NameNotFoundException, IOException, E {
        try (var container = getHandle(name).exclusive().orElseThrow()) {
            var oldValue = container.getNoThrow().orElseThrow(() -> new NameNotFoundException(name));
            var newValue = updater.apply(oldValue);
            container.update(newValue);
            synchronizedVoidLoader(loader -> loader.remember(name, Optional.of(newValue)));
            return newValue;
        }
    }


    protected void delete(@Nonnull String name) throws NameNotFoundException, IOException {
        var handle = getHandle(name);

        if (!handle.exists()) {
            throw new NameNotFoundException(name);
        }

        try (var container = handle.exclusive().orElseThrow()) {
            container.delete();
            synchronizedVoidLoader(loader -> loader.forget(name));
        }
    }
}
