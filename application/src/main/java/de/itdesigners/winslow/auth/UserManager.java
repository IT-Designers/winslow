package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class UserManager implements GroupAssignmentResolver {

    private static final Logger LOG = Logger.getLogger(UserManager.class.getSimpleName());

    private final @Nonnull Map<String, User> users = new HashMap<>();
    private final @Nonnull GroupManager      groups;

    public UserManager(@Nonnull GroupManager groups) {
        this.groups = groups;
        this.users.put(
                User.SUPER_USER_NAME,
                new User(
                        User.SUPER_USER_NAME,
                        this
                )
        );
    }

    @Nonnull
    public User createUserWithoutGroup(@Nonnull String name) throws InvalidNameException, NameAlreadyInUseException {
        InvalidNameException.ensureValid(name);
        NameAlreadyInUseException.ensureNotPresent(this.users.keySet(), name);

        var user = new User(name, this);
        this.users.put(name, user);
        return user;
    }

    @Nonnull
    public User createUserAndGroupIgnoreIfAlreadyExists(@Nonnull String name) throws InvalidNameException, IOException {
        try {
            return this.createUserAndGroup(name);
        } catch (NameAlreadyInUseException ignored) {
            return getUser(name).orElseThrow();
        }
    }

    @Nonnull
    public User createUserAndGroup(@Nonnull String name) throws InvalidNameException, NameAlreadyInUseException, IOException {
        var group = Prefix.User.wrap(Prefix.unwrap_or_given(name));
        var user  = this.createUserWithoutGroup(name);

        try {
            try {
                this.groups.createGroup(
                        group,
                        user.name(),
                        Role.OWNER
                );
            } catch (NameAlreadyInUseException ignored) {
                try {
                    this.groups.addOrUpdateMembership(group, user.name(), Role.OWNER);
                } catch (NameNotFoundException e) {
                    throw new IOException(e);
                }
            }


            return user;
        } catch (InvalidNameException | IOException e) {
            // cleanup: delete the user
            this.users.remove(name);
            throw e;
        }
    }

    /**
     * Doesn't care about {@link Prefix} or {@link InvalidNameException} - invalid inputs will always return
     * {@link Optional#empty()}
     *
     * @param name The name of the user to search for
     * @return If found, the representing {@link User} instance, otherwise {@link Optional#empty()}
     */
    @Nonnull
    public Optional<User> getUser(@Nonnull String name) {
        return Optional.ofNullable(this.users.get(name));
    }

    @Nonnull
    public Stream<User> getUsersPotentiallyIncomplete() {
        return this.users.values().stream();
    }

    /**
     * Tries to find the user for the given name (see {@link #getUser(String)}) or creates a user for the given name.
     * Will fail (return {@link Optional#empty()}) nonetheless if the given name is invalid, for example.
     *
     * @param name The name of the user to search or create
     * @return The {@link User} instance of the given name or {@link Optional#empty()} on an internal error.
     */
    @Nonnull
    public Optional<User> getUserOrCreateAuthenticated(@Nullable String name) {
        return Optional
                .ofNullable(name)
                .flatMap(n -> this.getUser(n).or(() -> {
                    try {
                        return Optional.of(this.createUserAndGroup(n));
                    } catch (NameAlreadyInUseException e) {
                        LOG.log(
                                Level.WARNING,
                                "Unexpected code path reached: user not found but trying to create it caused a " + NameAlreadyInUseException.class.getSimpleName(),
                                e
                        );
                        return Optional.empty();
                    } catch (InvalidNameException e) {
                        LOG.log(Level.WARNING, "Failed to create authenticated user", e);
                        return Optional.empty();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to persist changes", e);
                        return Optional.empty();
                    }
                }));
    }

    @Override
    public boolean isPartOfGroup(@Nonnull String user, @Nonnull String group) {
        return this.groups.getGroup(group).map(g -> g.isMember(user)).orElse(false);
    }

    @Nonnull
    @Override
    public List<Group> getAssignedGroups(@Nonnull String user) {
        return this.groups.getGroupsWithMember(user);
    }

    @Nonnull
    @Override
    public Optional<Group> getGroup(@Nonnull String name) {
        return this.groups.getGroup(name);
    }
}
