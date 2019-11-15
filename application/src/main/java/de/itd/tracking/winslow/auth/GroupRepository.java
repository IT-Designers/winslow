package de.itd.tracking.winslow.auth;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GroupRepository {

    public static final String SUPERGROUP = "root";

    private final Map<String, Group> groups = new HashMap<>();

    public GroupRepository() {
        this.withGroup(new Group(SUPERGROUP, true).withUser(UserRepository.SUPERUSER));
    }

    @Nonnull
    public Group createGroup(@Nonnull String name, boolean superGroup) {
        var group = new Group(name, superGroup);
        this.withGroup(group);
        return group;
    }

    @Nonnull
    private GroupRepository withGroup(@Nonnull Group group) {
        if (!this.groups.containsKey(group.getName())) {
            this.groups.put(group.getName(), group);
        }
        return this;
    }

    @Nonnull
    public Optional<Group> getGroup(String name) {
        return Optional.ofNullable(this.groups.get(name));
    }

    public Stream<Group> getGroupsWithMember(String user) {
        return this.groups.values().stream().filter(group -> group.isMember(user));
    }
}
