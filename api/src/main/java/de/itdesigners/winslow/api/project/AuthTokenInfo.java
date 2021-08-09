package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class AuthTokenInfo {

    public final @Nonnull String       id;
    public @Nullable      String       secret;
    public final @Nonnull String       name;
    public final @Nonnull List<String> capabilities;

    public AuthTokenInfo(
            @Nonnull String id,
            @Nullable String secret,
            @Nonnull String name,
            @Nonnull List<String> capabilities
    ) {
        this.id           = id;
        this.secret       = secret;
        this.name         = name;
        this.capabilities = capabilities;
    }

    @Nonnull
    public AuthTokenInfo withSecret(@Nonnull String secret) {
        this.secret = secret;
        return this;
    }
}
