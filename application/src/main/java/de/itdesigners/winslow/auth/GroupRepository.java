package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.CachedFunction;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

public class GroupRepository extends BaseRepository implements GroupPersistence {

    private static final String FILE_EXTENSION = ".yml";

    private final @Nonnull CachedFunction<String, Optional<Group>> cachedLoader;

    public GroupRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        this.cachedLoader = new CachedFunction<>(
                name -> getGroupHandle(name).unsafe().or(() -> {
                    // TODO remove?
                    // insert hook to always be able to retrieve the super-group
                    if (Group.SUPER_GROUP_NAME.equals(name)) {
                        return Optional.of(new Group(
                                name,
                                List.of(new Link(User.SUPER_USER_NAME, Role.OWNER))
                        ));
                    } else {
                        return Optional.empty();
                    }
                })
        );

        lockBus.registerEventListener(
                Event.Command.RELEASE,
                event -> {
                    var eventPath = Path.of(event.getSubject()).normalize();
                    var eventFile = eventPath.getFileName().toString();
                    var dir       = getRepositoryDirectory();
                    var parent    = workDirectoryConfiguration.getPath().resolve(eventPath).getParent().toUri();

                    if (parent.getPath().equals(dir.toUri().getPath()) && eventFile.endsWith(FILE_EXTENSION)) {
                        var groupName = eventFile.substring(0, eventFile.length() - FILE_EXTENSION.length());
                        synchronizedVoidLoader(loader -> loader.forget(groupName));
                    }
                },
                LockBus.RegistrationOption.NOTIFY_ONLY_IF_ISSUER_IS_NOT_US
        );
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return this.workDirectoryConfiguration.getAuthGroupDirectory();
    }

    @Nonnull
    private Handle<Group> getGroupHandle(@Nonnull String name) {
        return createHandle(getGroupFilePath(name), Group.class);
    }

    @Nonnull
    private Path getGroupFilePath(@Nonnull String name) {
        return getRepositoryDirectory().resolve(Path.of(name + FILE_EXTENSION).normalize().getFileName());
    }

    private synchronized <T> T synchronizedLoader(@Nonnull Function<CachedFunction<String, Optional<Group>>, T> callback) {
        return callback.apply(this.cachedLoader);
    }

    private void synchronizedVoidLoader(Consumer<CachedFunction<String, Optional<Group>>> callback) {
        this.synchronizedLoader(loader -> {
            callback.accept(loader);
            return null;
        });
    }

    @Nonnull
    @Override
    public Optional<Group> loadUnsafeNoThrows(@Nonnull String name) {
        return synchronizedLoader(loader -> loader.apply(name));
    }

    @Override
    public boolean storeIfNotExists(@Nonnull Group group) throws IOException {
        if (loadUnsafeNoThrows(group.name()).isEmpty()) {
            try (var container = getGroupHandle(group.name()).exclusive().orElseThrow()) {
                container.update(group);
            }
            synchronizedVoidLoader(loader -> loader.remember(group.name(), Optional.of(group)));
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    public <E extends Throwable> Group updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater) throws NameNotFoundException, IOException, E {
        try (var container = getGroupHandle(name).exclusive().orElseThrow()) {
            var oldGroup = container.getNoThrow().orElseThrow(() -> new NameNotFoundException(name));
            var newGroup = updater.apply(oldGroup);
            container.update(newGroup);
            synchronizedVoidLoader(loader -> loader.remember(name, Optional.of(newGroup)));
            return newGroup;
        }
    }

    @Override
    public void delete(@Nonnull String name) throws NameNotFoundException, IOException {
        var handle = getGroupHandle(name);

        if (!handle.exists()) {
            throw new NameNotFoundException(name);
        }

        try (var container = handle.exclusive().orElseThrow()) {
            container.delete();
            synchronizedVoidLoader(loader -> loader.forget(name));
        }
    }

    @Nonnull
    @Override
    public Stream<String> listGroupNamesNoThrows() {
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
            LOG.log(Level.WARNING, "Failed to list groups directory", e);
            return Stream.empty();
        }
    }
}
