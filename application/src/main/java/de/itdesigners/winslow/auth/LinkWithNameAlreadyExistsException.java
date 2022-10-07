package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class LinkWithNameAlreadyExistsException extends Exception {
    public LinkWithNameAlreadyExistsException(@Nonnull String name) {
        super("A Link for the name '" + name + "' already exists");
    }

    public static void ensureNotPresent(@Nonnull Link link, Stream<Link> stream) throws LinkWithNameAlreadyExistsException {
        ensureNotPresentInNames(link.name(), stream.map(Link::name));
    }

    public static void ensureNotPresentInNames(@Nonnull String name, Stream<String> stream) throws LinkWithNameAlreadyExistsException {
        if (stream.anyMatch(name::equals)) {
            throw new LinkWithNameAlreadyExistsException(name);
        }
    }
}
