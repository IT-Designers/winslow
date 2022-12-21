package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.CachedBaseRepository;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class UserRepository extends CachedBaseRepository<User> implements UserPersistence {

    public UserRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return this.workDirectoryConfiguration.getAuthUserDirectory();
    }

    @Nonnull
    @Override
    protected Class<User> getValueType() {
        return User.class;
    }

    @Nonnull
    @Override
    public Optional<User> loadUnsafeNoThrows(@Nonnull String name) {
        return super.loadUnsafeNoThrows(name);
    }

    @Nonnull
    @Override
    public Stream<String> listUserNamesNoThrows() {
        return super.listFileNamesWithoutFileExtensionNoThrows();
    }

    @Override
    public boolean storeIfNotExists(@Nonnull User user) throws IOException {
        return super.storeIfNotExists(user.name(), user);
    }

    @Nonnull
    @Override
    public <E extends Throwable> User updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<User, User, E> updater) throws NameNotFoundException, IOException, E {
        return super.updateComputeIfAbsent(name, updater);
    }

    @Override
    public void delete(@Nonnull String name) throws NameNotFoundException, IOException {
        super.delete(name);
    }
}
