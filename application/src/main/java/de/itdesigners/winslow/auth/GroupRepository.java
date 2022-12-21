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

public class GroupRepository extends CachedBaseRepository<Group> implements GroupPersistence {

    public GroupRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return this.workDirectoryConfiguration.getAuthGroupDirectory();
    }

    @Nonnull
    @Override
    protected Class<Group> getValueType() {
        return Group.class;
    }

    @Nonnull
    @Override
    public Optional<Group> loadUnsafeNoThrows(@Nonnull String name) {
        return super.loadUnsafeNoThrows(name);
    }

    @Nonnull
    @Override
    public Stream<String> listGroupNamesNoThrows() {
        return super.listFileNamesWithoutFileExtensionNoThrows();
    }

    @Override
    public boolean storeIfNotExists(@Nonnull Group group) throws IOException {
        return super.storeIfNotExists(group.name(), group);
    }

    @Nonnull
    @Override
    public <E extends Throwable> Group updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater) throws NameNotFoundException, IOException, E {
        return super.updateComputeIfAbsent(name, updater);
    }

    @Override
    public void delete(@Nonnull String name) throws NameNotFoundException, IOException {
        super.delete(name);
    }
}
