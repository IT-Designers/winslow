package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.auth.UserInfo;
import de.itdesigners.winslow.auth.InvalidPasswordException;
import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;

public class UserInfoConverter {

    @Nonnull
    public static UserInfo from(@Nonnull User user) {
        return new UserInfo(
                user.name(),
                user.displayName(),
                user.email(),
                user.active(),
                user.password() != null
                ? "*".repeat(InvalidPasswordException.MIN_LENGTH).toCharArray()
                : null
        );
    }
}
