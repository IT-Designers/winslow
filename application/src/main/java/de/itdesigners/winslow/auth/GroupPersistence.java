package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
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

    void store(@Nonnull Group group) throws IOException;

    void storeIfNotExists(@Nonnull Group group) throws NameAlreadyInUseException, IOException;

    @Nonnull
    default <E extends Throwable> Group update(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater
    ) throws NameNotFoundException, IOException, E {
        return updateComputeIfAbsent(name, updater, Optional::empty);
    }

    @Nonnull
    <E extends Throwable> Group updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater,
            @Nonnull Supplier<Optional<Group>> supplier
    ) throws NameNotFoundException, IOException, E;

    void delete(@Nonnull String name) throws NameNotFoundException, IOException;
}
