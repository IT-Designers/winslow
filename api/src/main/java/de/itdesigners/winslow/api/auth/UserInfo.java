package de.itdesigners.winslow.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record UserInfo(
        @Nonnull String name,
        @Nullable String displayName,
        @Nullable String email,
        @Nullable char[] password
) {
}
