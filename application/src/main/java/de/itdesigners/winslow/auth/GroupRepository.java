package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public class GroupRepository {

    private final Map<String, Group> groups = new HashMap<>();

    public GroupRepository() {
        this.groups.put(
                Group.SUPER_GROUP_NAME,
                new Group(
                        Group.SUPER_GROUP_NAME,
                        List.of(
                                new Link(User.SUPER_USER_NAME, Role.OWNER)
                        )
                )
        );
    }

    @Nonnull
    public Group createGroup(
            @Nonnull String name,
            @Nonnull String user,
            @Nonnull Role userRole) throws InvalidNameException, NameAlreadyInUseException {
        return createGroup(name, new Link(user, userRole));
    }

    @Nonnull
    public Group createGroup(
            @Nonnull String name,
            @Nonnull Link... members) throws InvalidNameException, NameAlreadyInUseException {
        return createGroup(name, List.of(members));
    }

    public Group createGroup(
            @Nonnull String name,
            @Nonnull List<Link> members) throws InvalidNameException, NameAlreadyInUseException {
        var group = new Group(name, Collections.unmodifiableList(members));
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

    public void addMemberToGroup(
            @Nonnull String group,
            @Nonnull String user,
            @Nonnull Role role) throws NameNotFoundException, LinkWithNameAlreadyExistsException {
        this.addMemberToGroup(group, new Link(user, role));
    }

    public void addMemberToGroup(
            @Nonnull String group,
            @Nonnull Link link) throws NameNotFoundException, LinkWithNameAlreadyExistsException {
        var oldGroup = Optional.ofNullable(this.groups.get(group)).orElseThrow(() -> new NameNotFoundException(group));

        if (oldGroup.getMembers().stream().anyMatch(l -> l.name().equals(link.name()))) {
            throw new LinkWithNameAlreadyExistsException(link.name());
        }


        var newMembers = new ArrayList<>(oldGroup.getMembers());
        newMembers.add(link);
        this.groups.put(group, new Group(group, newMembers));
    }

    @Nonnull
    public Optional<Group> getGroup(String name) {
        return Optional.ofNullable(this.groups.get(name));
    }

    @Nonnull
    public Stream<Group> getGroupsWithMember(String user) {
        return this.groups.values().stream().filter(group -> group.isMember(user));
    }
}
