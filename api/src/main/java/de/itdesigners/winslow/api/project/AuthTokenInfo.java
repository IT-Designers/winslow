package de.itdesigners.winslow.api.project;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.List;
import java.util.Optional;

public record AuthTokenInfo(
        @Nonnull String id,
        @Nullable String secret,
        @Nonnull String name,
        @Nonnull List<String> capabilities) {

    @Nonnull
    @Transient
    public Optional<String> optSecret() {
        return Optional.ofNullable(secret);
    }

    @Nonnull
    @Transient
    @CheckReturnValue
    public AuthTokenInfo withSecret(@Nonnull String secret) {
        return new AuthTokenInfo(this.id, secret, this.name, this.capabilities);
    }
}
