package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.util.ThrowingFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DummyGroupPersistence implements GroupPersistence {

    private final @Nonnull HashMap<String, Group> groups = new HashMap<>();

    @Nonnull
    @Override
    public Optional<Group> loadUnsafeNoThrows(@Nonnull String name) {
        return Optional.ofNullable(this.groups.get(name));
    }

    @Nonnull
    @Override
    public Stream<String> listGroupNamesNoThrows() {
        return this.groups.keySet().stream();
    }

    @Override
    public void store(@Nonnull Group group) throws IOException {
        this.groups.put(group.name(), group);
    }

    @Override
    public void storeIfNotExists(@Nonnull Group group) throws NameAlreadyInUseException, IOException {
        NameAlreadyInUseException.ensureNotPresent(group.name(), this.groups.keySet().stream());
        this.groups.put(group.name(), group);
    }

    @Nonnull
    @Override
    public <E extends Throwable> Group updateComputeIfAbsent(
            @Nonnull String name,
            @Nonnull ThrowingFunction<Group, Group, E> updater,
            @Nonnull Supplier<Optional<Group>> supplier) throws NameNotFoundException, IOException, E {
        var result = updater.apply(
                this.loadUnsafeNoThrows(name)
                    .or(supplier)
                    .orElseThrow(() -> new org.springframework.ldap.NameNotFoundException(name))
        );
        this.groups.put(name, result);
        return result;
    }

    @Override
    public void delete(@Nonnull String name) throws IOException {
        this.groups.remove(name);
    }
}
