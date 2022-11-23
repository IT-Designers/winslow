package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

public class NameNotFoundException extends Exception {
    public NameNotFoundException(@Nonnull String name) {
        super("The name '" + name + "' was not found");
    }


    public static void ensurePresent(@Nonnull Set<String> keySet, @Nonnull String name) throws NameNotFoundException{
        if (keySet.stream().noneMatch(n -> Objects.equals(n, name))) {
            throw new NameNotFoundException(name);
        }
    }
}
