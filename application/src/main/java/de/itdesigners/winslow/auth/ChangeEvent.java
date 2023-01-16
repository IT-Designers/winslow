package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;

public record ChangeEvent<T>(
        @Nonnull Subject subject,
        @Nonnull T value) {

    public interface Listener<T> {
        void onEvent(@Nonnull ChangeEvent<T> event);
    }

    public enum Subject {
        DELETED
    }
}
