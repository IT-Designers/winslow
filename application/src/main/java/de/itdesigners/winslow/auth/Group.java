package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * Current snapshot of a group. An instance is not intended to be cached in memory.
 * A {@link Group} instance will not be updated if the group is changed in the backend,
 * thus not reflecting any changes after instantiation.
 * <br>
 * Provides convenience functions such as {@link #isSuperGroup()}, {@link #getRole(String)} and {@link #isMember(String)}.
 *
 * @param getName The name (might be {@link Prefix}ed)
 * @param getMembers An immutable {@link List} of {@link Link}ed members
 */
public record Group(
        @Nonnull String getName,
        @Nonnull List<Link> getMembers
) {

    /**
     * The name of the group with super privileges / the name of the group that is privileged.
     * Similar to the root-user in UNIX like systems.
     */
    public static final String SUPER_GROUP_NAME = "root";

    /**
     * @return Whether this {@link Group} is privileged, see {@link #SUPER_GROUP_NAME}.
     */
    public boolean isSuperGroup() {
        return SUPER_GROUP_NAME.equals(this.getName());
    }

    /**
     * Does not care about any {@link Prefix} wrapping / unwrapping. The given name
     * is compared with the members-list as given.
     *
     * @param name The name of the member to search for
     * @return Whether there is a member entry for the given name
     */
    public boolean isMember(@Nonnull String name) {
        return this.getMembers().stream().anyMatch(link -> link.name().equals(name));
    }

    /**
     * param name The name of the member to search for
     * @return If found, the {@link Role} for the given name, otherwise {@link Optional#empty()}
     */
    @Nonnull
    public Optional<Role> getRole(@Nonnull String name) {
        return this.getMembers()
                .stream()
                .filter(link -> link.name().equals(name))
                .findFirst()
                .map(Link::role);
    }
}
