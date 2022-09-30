package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.Optional;

public enum Prefix {
    /**
     * Used for system {@link User}s and {@link Group}s
     */
    System("system"),
    User("user");

    public static final @Nonnull String SEPARATOR = "::";

    private final @Nonnull String prefix;

    Prefix(@Nonnull String prefix) {
        this.prefix = prefix;
    }

    @Nonnull
    public String wrap(@Nonnull String name) {
        return this.prefix + SEPARATOR + name;
    }

    @Nonnull
    public static Optional<String> unwrap(@Nonnull String wrapped) {
        int index = wrapped.lastIndexOf(SEPARATOR);
        if (index >= 0) {
            return Optional.of(wrapped.substring(index + SEPARATOR.length()));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public static String unwrap_or_given(@Nonnull String wrapped) {
        return Prefix.unwrap(wrapped).orElse(wrapped);
    }
}
