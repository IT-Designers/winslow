package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class LinkWithNameNotFoundException extends Exception {
    public LinkWithNameNotFoundException(@Nonnull String name) {
        super("There is no link with the '" + name + "'");
    }

    public static void ensurePresent(@Nonnull Link link, @Nonnull Stream<Link> stream) throws LinkWithNameNotFoundException {
        ensurePresent(link.name(), stream.map(Link::name));
    }

    public static void ensurePresent(@Nonnull String name, @Nonnull Stream<String> stream) throws LinkWithNameNotFoundException {
        if (stream.noneMatch(name::equals)) {
            throw new LinkWithNameNotFoundException(name);
        }
    }
}
