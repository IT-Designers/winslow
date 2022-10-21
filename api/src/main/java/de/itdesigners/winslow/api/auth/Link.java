package de.itdesigners.winslow.api.auth;

import javax.annotation.Nonnull;

public record Link(@Nonnull String name, @Nonnull Role role) {


    /**
     * Helper that creates {@link Link} with {@link Role#MEMBER} for the given name
     * @param name The name to create the link for
     * @return The new {@link Link} with the given name and {@link Role#MEMBER}
     */
    @Nonnull
    public static Link member(@Nonnull String name) {
        return new Link(name, Role.MEMBER);
    }

    /**
     * Helper that creates {@link Link} with {@link Role#OWNER} for the given name
     * @param name The name to create the link for
     * @return The new {@link Link} with the given name and {@link Role#OWNER}
     */
    @Nonnull
    public static Link owner(@Nonnull String name) {
        return new Link(name, Role.OWNER);
    }

}
