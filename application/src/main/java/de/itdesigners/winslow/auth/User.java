package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.settings.ResourceLimitation;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class User {

    private final String                  name;
    private final boolean                 superUser;
    private final GroupAssignmentResolver resolver;

    public User(String name, boolean superUser, GroupAssignmentResolver resolver) {
        this.name      = name;
        this.superUser = superUser;
        this.resolver  = resolver;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return Whether this user is a super user by itself
     */
    public boolean isSuperUser() {
        return this.superUser;
    }

    /**
     * @return Whether this user has super privileges either by being a super user itself
     *         or by inheriting super privileges through an assigned group
     */
    public boolean hasSuperPrivileges() {
        return this.isSuperUser()
                || this.getGroups().map(resolver::getGroup).flatMap(Optional::stream).anyMatch(Group::isSuperGroup);
    }

    public boolean canAccessGroup(String group) {
        return this.resolver.canAccessGroup(this.name, group);
    }

    @Nonnull
    public Stream<String> getGroups() {
        return this.resolver.getAssignedGroups(this.name);
    }

    @Nonnull
    public Optional<ResourceLimitation> getResourceLimitation() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "'}#" + hashCode();
    }
}
