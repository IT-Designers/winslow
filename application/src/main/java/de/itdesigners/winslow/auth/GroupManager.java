package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GroupManager {

    private static final @Nonnull Group SUPER_GROUP = new Group(
            Group.SUPER_GROUP_NAME,
            List.of(new Link(User.SUPER_USER_NAME, Role.OWNER))
    );

    private final @Nonnull GroupPersistence persistence;

    public GroupManager(@Nonnull GroupPersistence persistence) {
        this.persistence = persistence;
    }

    @Nonnull
    public Group createGroup(
            @Nonnull String name,
            @Nonnull String user,
            @Nonnull Role userRole) throws InvalidNameException, NameAlreadyInUseException, IOException {
        return createGroup(name, new Link(user, userRole));
    }

    @Nonnull
    public Group createGroup(
            @Nonnull String name,
            @Nonnull Link... members) throws InvalidNameException, NameAlreadyInUseException, IOException {
        return createGroup(name, List.of(members));
    }

    public Group createGroup(
            @Nonnull String name,
            @Nonnull List<Link> members) throws InvalidNameException, NameAlreadyInUseException, IOException {
        InvalidNameException.ensureValid(name);
        NameAlreadyInUseException.ensureNotPresent(name, this.persistence.listGroupNamesNoThrows());
        var group = new Group(name, Collections.unmodifiableList(members));
        this.persistence.store(group);
        return group;
    }

    @Nonnull
    public Group createGroupIfNotExists(@Nonnull Group group) throws InvalidNameException, IOException {
        try {
            InvalidNameException.ensureValid(group.name());
            this.persistence.storeIfNotExists(group);
            return group;
        } catch (NameAlreadyInUseException e) {
            return this.persistence.loadUnsafeNoThrows(group.name()).orElseThrow();
        }
    }

    public void addMemberToGroup(
            @Nonnull String group,
            @Nonnull String user,
            @Nonnull Role role) throws NameNotFoundException, LinkWithNameAlreadyExistsException, IOException {
        this.addMemberToGroup(group, new Link(user, role));
    }

    public void addMemberToGroup(
            @Nonnull String group,
            @Nonnull Link link) throws NameNotFoundException, LinkWithNameAlreadyExistsException, IOException {
        var unsafeGroup = getGroup(group);
        LinkWithNameAlreadyExistsException.ensureNotPresent(
                link,
                unsafeGroup.stream().flatMap(g -> g.members().stream())
        );

        this.persistence.updateComputeIfAbsent(group, oldGroup -> {
            LinkWithNameAlreadyExistsException.ensureNotPresent(link, oldGroup.members().stream());

            return new Group(
                    oldGroup.name(),
                    Stream.concat(
                            oldGroup.members().stream(),
                            Stream.of(link)
                    ).toList()
            );
        }, () -> getSuperGroupSupplier(group));
    }

    public Group addOrUpdateMembership(
            @Nonnull String group,
            @Nonnull String user,
            @Nonnull Role role) throws NameNotFoundException, IOException, InvalidNameException {
        return addOrUpdateMembership(group, new Link(user, role));
    }

    public Group addOrUpdateMembership(
            @Nonnull String group,
            @Nonnull Link link) throws NameNotFoundException, IOException, InvalidNameException {
        InvalidNameException.ensureValid(link.name());

        // nothing to do?
        var current = getGroup(group).filter(g -> g.hasMemberWithRole(link));
        if (current.isPresent()) {
            return current.get();
        }

        return this.persistence.updateComputeIfAbsent(group, oldGroup -> new Group(
                oldGroup.name(),
                Stream.concat(
                        oldGroup
                                .members()
                                .stream()
                                .filter(l -> !l.name().equals(link.name())),
                        Stream.of(link)
                ).toList()
        ), () -> getSuperGroupSupplier(group));
    }

    @Nonnull
    public Optional<Group> getGroup(@Nonnull String name) {
        return this.persistence
                .loadUnsafeNoThrows(name)
                .or(() -> getSuperGroupSupplier(name));
    }

    @Nonnull
    public List<Group> getGroups() {
        try (var stream = this.persistence.listGroupNamesNoThrows()) {
            return stream
                    .flatMap(name -> getGroup(name).stream())
                    .toList();
        }
    }

    @Nonnull
    public List<Group> getGroupsWithMember(@Nonnull String user) {
        try (var stream = this.persistence.listGroupNamesNoThrows()) {
            return Stream.concat(
                                 Stream.of(SUPER_GROUP.name()),
                                 stream.filter(name -> !SUPER_GROUP.name().equals(name))
                         )
                         .flatMap(name -> getGroup(name).stream())
                         .filter(group -> group.isMember(user))
                         .toList();
        }
    }

    public void deleteGroup(@Nonnull String name) throws NameNotFoundException, IOException {
        this.persistence.delete(name);
    }

    public void deleteMembership(@Nonnull String name, @Nonnull String user) throws IOException, NameNotFoundException, LinkWithNameNotFoundException {
        LinkWithNameNotFoundException.ensurePresent(
                user,
                this.persistence
                        .loadUnsafeNoThrows(name)
                        .stream()
                        .flatMap(g -> g.members().stream().map(Link::name))
        );

        this.persistence.update(name, oldGroup -> new Group(
                oldGroup.name(),
                oldGroup.members().stream().filter(link -> !link.name().equalsIgnoreCase(user)).toList()
        ));
    }

    @Nonnull
    private static Optional<Group> getSuperGroupSupplier(@Nonnull String group) {
        if (SUPER_GROUP.name().equals(group)) {
            return Optional.of(SUPER_GROUP);
        } else {
            return Optional.empty();
        }
    }
}
