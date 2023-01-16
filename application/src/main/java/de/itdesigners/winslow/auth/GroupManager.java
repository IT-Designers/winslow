package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class GroupManager {

    private final @Nonnull GroupPersistence                                            persistence;
    private final @Nonnull Map<ChangeEvent.Subject, List<ChangeEvent.Listener<Group>>> listeners = new HashMap<>();

    public GroupManager(@Nonnull GroupPersistence persistence) throws IOException {
        this.persistence = persistence;
        this.persistence.storeIfNotExists(new Group(
                Group.SUPER_GROUP_NAME,
                List.of(new Link(User.SUPER_USER_NAME, Role.OWNER))
        ));
    }

    public void addChangeListener(@Nonnull ChangeEvent.Subject subject, @Nonnull ChangeEvent.Listener<Group> listener) {
        this.listeners.computeIfAbsent(subject, s -> new ArrayList<>()).add(listener);
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
        var group = new Group(name, Collections.unmodifiableList(members));
        this.persistence.store(group);
        return group;
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
        });
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
        ));
    }

    @Nonnull
    public Optional<Group> getGroup(@Nonnull String name) {
        return this.persistence.loadUnsafeNoThrows(name);
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
            return stream
                    .flatMap(name -> getGroup(name).stream())
                    .filter(group -> group.isMember(user))
                    .toList();
        }
    }

    public void deleteGroup(@Nonnull String name) throws NameNotFoundException, IOException {
        var loaded = this.persistence.loadUnsafeNoThrows(name);
        this.persistence.delete(name);

        loaded
                .map(group -> new ChangeEvent<>(ChangeEvent.Subject.DELETED, group))
                .ifPresent(event -> {
                    Stream
                            .ofNullable(this.listeners.get(event.subject()))
                            .flatMap(Collection::stream)
                            .forEach(listener -> listener.onEvent(event));
                });
    }

    public void deleteMembership(
            @Nonnull String name,
            @Nonnull String user) throws IOException, NameNotFoundException, LinkWithNameNotFoundException {
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
}
