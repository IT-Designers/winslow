package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class LinkAlreadyExistsException extends Exception {
    public LinkAlreadyExistsException(@Nonnull Link link) {
        super("A Link with the name='" + link.name() + "' and role='" + link.role() + "' already exists");
    }

    public static void ensureNotPresent(@Nonnull Link link, @Nonnull Stream<Link> stream) throws LinkAlreadyExistsException {
        if (stream.anyMatch(link::equals)) {
            throw new LinkAlreadyExistsException(link);
        }
    }
}
