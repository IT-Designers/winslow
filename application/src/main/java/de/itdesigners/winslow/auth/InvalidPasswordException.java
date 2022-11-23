package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;

public class InvalidPasswordException extends Exception {

    public static final int MIN_LENGTH = 8;

    public InvalidPasswordException(int length) {
        super("The password is too short, expected at least " + MIN_LENGTH + " characters, but got " + length);
    }

    @Nonnull
    public static char[] ensureValid(@Nonnull char[] password) throws InvalidPasswordException {
        if (password.length < MIN_LENGTH) {
            throw new InvalidPasswordException(password.length);
        } else {
            return password;
        }
    }
}
