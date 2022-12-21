package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserPersistence {

    @Nonnull
    Optional<User> loadUnsafeNoThrows(@Nonnull String name);

    /**
     * IO-Stream, needs to be closed manually!
     *
     * @return A {@link Stream} over all names of available {@link User}s
     */
    @Nonnull
    Stream<String> listUserNamesNoThrows();

    default void store(@Nonnull User user) throws IOException, NameAlreadyInUseException {
        if (!storeIfNotExists(user)) {
            throw new NameAlreadyInUseException(user.name());
        }
    }

    /**
     * @param user The {@link User} to store
     * @return Whether the given {@link User} was stored, or false if there already exists a {@link User} with the same name
     * @throws IOException If serializing or writing the {@link User} failed
     */
    boolean storeIfNotExists(@Nonnull User user) throws IOException;
    
    @Nonnull
    default <E extends Throwable> User update(
            @Nonnull String name,
            @Nonnull ThrowingFunction<User, User, E> updater
    ) throws NameNotFoundException, IOException, E {
        return updateComputeIfAbsent(name, updater);
    }

    @Nonnull
    <E extends Throwable> User updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<User, User, E> updater
    ) throws NameNotFoundException, IOException, E;

    void delete(@Nonnull String name) throws NameNotFoundException, IOException;
}
