package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class UserRepository implements GroupAssignmentResolver {

    public static final String SUPERUSER = "root";

    private final Map<String, User> users = new HashMap<>();
    private final GroupRepository   groups;

    public UserRepository(GroupRepository groups) {
        this.groups = groups;
        this.createUser(SUPERUSER, true);

    }

    @Nonnull
    public User createUser(@Nonnull String name, boolean superUser) {
        var user = new User(name, superUser, this);
        this.withUser(user);
        return user;
    }

    @Nonnull
    private UserRepository withUser(@Nonnull User user) {
        if (!this.users.containsKey(user.getName())) {
            this.users.put(user.getName(), user);
        }
        return this;
    }

    @Nonnull
    public Optional<User> getUser(String name) {
        return Optional.ofNullable(this.users.get(name));
    }

    @Nonnull
    public Optional<User> getUserOrCreateAuthenticated(String name) {
        var user = this.users.get(name);
        if (user == null && name != null) {
            user = new User(name, false, this);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public boolean canAccessGroup(@Nonnull String user, @Nonnull String group) {
        return SUPERUSER.equals(user)
                || user.equals(group)
                || groups.getGroup(group).map(g -> g.isMember(user)).orElse(false);
    }

    @Nonnull
    @Override
    public Stream<String> getAssignedGroups(@Nonnull String user) {
        return Stream.concat(Stream.of(user), this.groups.getGroupsWithMember(user).map(Group::getName));
    }

    @Nonnull
    @Override
    public Optional<Group> getGroup(@Nonnull String name) {
        return this.groups.getGroup(name);
    }
}
