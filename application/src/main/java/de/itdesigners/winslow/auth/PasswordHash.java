package de.itdesigners.winslow.auth;

import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.annotation.Nonnull;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public record PasswordHash(
        @Nonnull String hash
) {
    public static int DEFAULT_ROUNDS         = 14;
    public static int BCRYPT_MAX_BYTE_LENGTH = 72;

    public boolean isPasswordCorrect(@Nonnull String password) {
        return isPasswordCorrect(password.toCharArray());
    }

    public boolean isPasswordCorrect(@Nonnull char[] password) {
        return BCrypt.checkpw(toUtf8Bytes(password), hash);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull String password) {
        return calculate(password, DEFAULT_ROUNDS);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull String password, int rounds) {
        return calculate(password.toCharArray(), rounds);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull char[] password) {
        return calculate(password, DEFAULT_ROUNDS);
    }

    @Nonnull
    public static PasswordHash calculate(@Nonnull char[] password, int rounds) {
        var salt  = BCrypt.gensalt(rounds);
        var bytes = toUtf8Bytes(password);

        if (bytes.length > BCRYPT_MAX_BYTE_LENGTH) {
            throw new RuntimeException("Password is too long for BCrypt, max bytes: " + BCRYPT_MAX_BYTE_LENGTH);
        }

        return new PasswordHash(BCrypt.hashpw(bytes, salt));
    }

    @Nonnull
    private static byte[] toUtf8Bytes(@Nonnull char[] chars) {
        var buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        var bytes  = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }
}
