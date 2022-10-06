package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.beans.Transient;
import java.util.List;
import java.util.Optional;

/**
 * Current snapshot of a group. An instance is not intended to be cached in memory.
 * A {@link Group} instance will not be updated if the group is changed in the backend,
 * thus not reflecting any changes after instantiation.
 * <br>
 * Provides convenience functions such as {@link #isSuperGroup()}, {@link #getRole(String)} and {@link #isMember(String)}.
 *
 * @param name    The name (might be {@link Prefix}ed)
 * @param members An immutable {@link List} of {@link Link}ed members
 */
public record Group(
        @Nonnull String name,
        @Nonnull List<Link> members
) {

    /**
     * The name of the group with super privileges / the name of the group that is privileged.
     * Similar to the root-user in UNIX like systems.
     */
    public static final String SUPER_GROUP_NAME = "root";

    /**
     * @return Whether this {@link Group} is privileged, see {@link #SUPER_GROUP_NAME}.
     */
    @Transient
    public boolean isSuperGroup() {
        return SUPER_GROUP_NAME.equals(this.name());
    }

    /**
     * Does not care about any {@link Prefix} wrapping / unwrapping. The given name
     * is compared with the members-list as given.
     *
     * @param name The name of the member to search for
     * @return Whether there is a member entry for the given name
     */
    public boolean isMember(@Nonnull String name) {
        return this.members().stream().anyMatch(link -> link.name().equals(name));
    }

    /**
     * Does not care about any {@link Prefix} wrapping / unwrapping. The given name
     * is compared with the members-list as given.
     *
     * @param name The name of the member to search for
     * @param role Expected {@link Role} of the member
     * @return Whether there is a member entry for the given name and {@link Role}
     */
    public boolean hasMemberWithRole(@Nonnull String name, @Nonnull Role role) {
        return hasMemberWithRole(new Link(name, role));
    }

    /**
     * Does not care about any {@link Prefix} wrapping / unwrapping. The given name
     * is compared with the members-list as given.
     *
     * @param link The name and {@link Role} of the member to search for
     * @return Whether there is a member entry for the given name and {@link Role}
     */
    public boolean hasMemberWithRole(@Nonnull Link link) {
        return this
                .members()
                .stream()
                .anyMatch(link::equals);
    }

    /**
     * param name The name of the member to search for
     *
     * @return If found, the {@link Role} for the given name, otherwise {@link Optional#empty()}
     */
    @Nonnull
    public Optional<Role> getRole(@Nonnull String name) {
        return this.members()
                   .stream()
                   .filter(link -> link.name().equals(name))
                   .findFirst()
                   .map(Link::role);
    }
}
