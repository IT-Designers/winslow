package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;

public class NameNotFoundException extends Exception {
    public NameNotFoundException(@Nonnull String name) {
        super("The name '" + name + "' was not found");
    }
}
