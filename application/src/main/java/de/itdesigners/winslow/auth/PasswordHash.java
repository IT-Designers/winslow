package de.itdesigners.winslow.auth;

import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.annotation.Nonnull;

public record PasswordHash(
        @Nonnull String hash
) {
    public static int DEFAULT_ROUNDS = 14;

    public boolean isPasswordCorrect(@Nonnull String password) {
        return BCrypt.checkpw(password, hash);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull String password) {
        return calculate(password, DEFAULT_ROUNDS);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull String password, int rounds) {
        var salt = BCrypt.gensalt(rounds);
        return new PasswordHash(BCrypt.hashpw(password, salt));
    }

}
