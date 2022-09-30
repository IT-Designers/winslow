package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.settings.ResourceLimitation;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Current snapshot of a user. An instance is not intended to be cached in memory.
 * A {@link User} instance will not be updated if the user is changed in the backend,
 * thus not reflecting any changes after instantiation.
 * <br>
 * Provides convenience functions such as {@link #isSuperUser()}, {@link #hasSuperPrivileges()} (String)} and
 * {@link #isPartOfGroup(String)}.
 *
 * @param getName                    The name (might be {@link Prefix}ed)
 * @param getGroupAssignmentResolver Helper to resolve group assignment requests
 */
public record User(
        @Nonnull String getName,
        @Nonnull GroupAssignmentResolver getGroupAssignmentResolver
) {

    /**
     * The name of the user with super privileges / the name of the user that is privileged.
     * Similar to the root-user in UNIX like systems.
     */
    public static final String SUPER_USER_NAME = "root";

    /**
     * @return Whether this {@link User} is privileged through its name, see {@link #SUPER_USER_NAME}
     */
    public boolean isSuperUser() {
        return SUPER_USER_NAME.equals(this.getName());
    }

    /**
     * @return Whether this user has super privileges either by {@link #isSuperUser()}
     * or by inheriting super privileges through an assigned group
     */
    public boolean hasSuperPrivileges() {
        return this.isSuperUser() || this.getGroups().anyMatch(Group::isSuperGroup);
    }

    /**
     * @return Names of {@link Group}s that list this {@link User} as member.
     */
    @Nonnull
    public Stream<Group> getGroups() {
        return this.getGroupAssignmentResolver().getAssignedGroups(this.getName());
    }

    /**
     * Does not care about any {@link Prefix} wrapping / unwrapping. The given name
     * is compared with the members-list as given.
     *
     * @param group The name of the {@link Group} to test for
     * @return Whether this user is a member of the group with the given name
     */
    public boolean isPartOfGroup(@Nonnull String group) {
        return this.getGroupAssignmentResolver().isPartOfGroup(this.getName(), group);
    }

    @Nonnull
    public Optional<ResourceLimitation> getResourceLimitation() {
        return Optional.empty();
    }
}
