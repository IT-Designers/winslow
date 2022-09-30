package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.Collection;

public class NameAlreadyInUseException extends Exception {
    public NameAlreadyInUseException(@Nonnull String name) {
        super("The name '" + name + "' is already in use");
    }

    public static void ensureNotPresent(@Nonnull Collection<String> collection, @Nonnull String name) throws NameAlreadyInUseException {
        if (collection.contains(name)) {
            throw new NameAlreadyInUseException(name);
        }
    }
}
