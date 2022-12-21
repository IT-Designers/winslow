package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.CachedFunction;
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

public class UserRepository extends BaseRepository implements UserPersistence {

    private static final String FILE_EXTENSION = ".yml";

    private final @Nonnull CachedFunction<String, Optional<User>> cachedLoader;

    public UserRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        this.cachedLoader = new CachedFunction<>(name -> getUserHandle(name).unsafe());

        lockBus.registerEventListener(
                Event.Command.RELEASE,
                event -> {
                    var eventPath = Path.of(event.getSubject()).normalize();
                    var eventFile = eventPath.getFileName().toString();
                    var dir       = getRepositoryDirectory();
                    var parent    = workDirectoryConfiguration.getPath().resolve(eventPath).getParent().toUri();

                    if (parent.getPath().equals(dir.toUri().getPath()) && eventFile.endsWith(FILE_EXTENSION)) {
                        var userName = eventFile.substring(0, eventFile.length() - FILE_EXTENSION.length());
                        synchronizedVoidLoader(loader -> loader.forget(userName));
                    }
                },
                LockBus.RegistrationOption.NOTIFY_ONLY_IF_ISSUER_IS_NOT_US
        );
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return this.workDirectoryConfiguration.getAuthUserDirectory();
    }

    @Nonnull
    private Handle<User> getUserHandle(@Nonnull String name) {
        return createHandle(getUserFilePath(name), User.class);
    }

    @Nonnull
    private Path getUserFilePath(@Nonnull String name) {
        return getRepositoryDirectory().resolve(Path.of(name + FILE_EXTENSION).normalize().getFileName());
    }

    private synchronized <T> T synchronizedLoader(@Nonnull Function<CachedFunction<String, Optional<User>>, T> callback) {
        return callback.apply(this.cachedLoader);
    }

    private void synchronizedVoidLoader(Consumer<CachedFunction<String, Optional<User>>> callback) {
        this.synchronizedLoader(loader -> {
            callback.accept(loader);
            return null;
        });
    }

    @Nonnull
    @Override
    public Optional<User> loadUnsafeNoThrows(@Nonnull String name) {
        return synchronizedLoader(loader -> loader.apply(name));
    }

    @Nonnull
    @Override
    public Stream<String> listUserNamesNoThrows() {
        Function<Stream<Path>, Stream<String>> noneThrowingMapping = stream -> stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(FILE_EXTENSION))
                .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                .toList()
                .stream();

        try (var stream = Files.list(getRepositoryDirectory())) {
            return noneThrowingMapping.apply(stream);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list users directory", e);
            return Stream.empty();
        }
    }

    @Override
    public boolean storeIfNotExists(@Nonnull User user) throws IOException {
        if (loadUnsafeNoThrows(user.name()).isEmpty()) {
            try (var container = getUserHandle(user.name()).exclusive().orElseThrow()) {
                container.update(user);
            }
            synchronizedVoidLoader(loader -> loader.remember(user.name(), Optional.of(user)));
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    public <E extends Throwable> User updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<User, User, E> updater) throws NameNotFoundException, IOException, E {
        try (var container = getUserHandle(name).exclusive().orElseThrow()) {
            var oldGroup = container.getNoThrow().orElseThrow(() -> new org.springframework.ldap.NameNotFoundException(name));
            var newGroup = updater.apply(oldGroup);
            container.update(newGroup);
            synchronizedVoidLoader(loader -> loader.remember(name, Optional.of(newGroup)));
            return newGroup;
        }
    }

    @Override
    public void delete(@Nonnull String name) throws NameNotFoundException, IOException {
        var handle = getUserHandle(name);

        if (!handle.exists()) {
            throw new NameNotFoundException(name);
        }

        try (var container = handle.exclusive().orElseThrow()) {
            container.delete();
            synchronizedVoidLoader(loader -> loader.forget(name));
        }
    }
}
