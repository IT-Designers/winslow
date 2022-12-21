package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public interface GroupPersistence {
    @Nonnull
    Optional<Group> loadUnsafeNoThrows(@Nonnull String name);

    /**
     * IO-Stream, needs to be closed manually!
     *
     * @return A {@link Stream} over all names of available {@link Group}s
     */
    @Nonnull
    Stream<String> listGroupNamesNoThrows();

    default void store(@Nonnull Group group) throws IOException, NameAlreadyInUseException {
        if (!storeIfNotExists(group)) {
            throw new NameAlreadyInUseException(group.name());
        }
    }

    /**
     * @param group The {@link Group} to store
     * @return Whether the given {@link Group} was stored, or false if there already exists a {@link Group} with the same name
     * @throws IOException If serializing or writing the {@link Group} failed
     */
    boolean storeIfNotExists(@Nonnull Group group) throws IOException;

    @Nonnull
    default <E extends Throwable> Group update(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater
    ) throws NameNotFoundException, IOException, E {
        return updateComputeIfAbsent(name, updater);
    }

    @Nonnull
    <E extends Throwable> Group updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater
    ) throws NameNotFoundException, IOException, E;

    void delete(@Nonnull String name) throws NameNotFoundException, IOException;
}
