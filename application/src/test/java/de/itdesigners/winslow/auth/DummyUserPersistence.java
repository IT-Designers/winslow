package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class DummyUserPersistence implements UserPersistence {

    private final @Nonnull Map<String, User> users = new HashMap<>();

    @Nonnull
    @Override
    public Optional<User> loadUnsafeNoThrows(@Nonnull String name) {
        return Optional.ofNullable(this.users.get(name));
    }

    @Nonnull
    @Override
    public Stream<String> listUserNamesNoThrows() {
        return this.users.keySet().stream();
    }

    @Override
    public boolean storeIfNotExists(@Nonnull User user) {
        if (!this.users.containsKey(user.name())) {
            this.users.put(user.name(), user);
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    public <E extends Throwable> User updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<User, User, E> updater) throws NameNotFoundException, E {
        var user = this.users.get(name);
        if (user == null) {
            throw new NameNotFoundException(name);
        } else {
            user = updater.apply(user);
            this.users.put(name, user);
            return user;
        }
    }

    @Override
    public void delete(@Nonnull String name) throws NameNotFoundException {
        if (this.users.remove(name) == null) {
            throw new NameNotFoundException(name);
        }
    }
}
