package de.itdesigners.winslow.web.websocket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChangeEvent<T, V> {
    private final @Nonnull  ChangeType type;
    private final @Nonnull  T          identifier;
    private final @Nullable V          value;

    public ChangeEvent(@Nonnull ChangeType type, @Nonnull T identifier, @Nullable V value) {
        this.type       = type;
        this.identifier = identifier;
        this.value      = value;
    }

    @Nonnull
    public ChangeType getType() {
        return type;
    }

    @Nonnull
    public T getIdentifier() {
        return identifier;
    }

    @Nullable
    public V getValue() {
        return value;
    }

    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE
    }
}
