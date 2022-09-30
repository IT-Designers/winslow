package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;

public class LinkWithNameAlreadyExistsException extends Exception {
    public LinkWithNameAlreadyExistsException(@Nonnull String name) {
        super("A Link for the name '" + name + "' already exists");
    }
}
