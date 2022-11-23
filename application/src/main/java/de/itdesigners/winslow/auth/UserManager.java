package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Role;

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
                        null,
                        null,
                        null,
                        this

                )
        );
    }

    @Nonnull
    public User createUserWithoutGroup(@Nonnull String name) throws InvalidNameException, NameAlreadyInUseException, IOException {
        try {
            return this.createUserWithoutGroup(name, null, null, null);
        } catch (InvalidPasswordException e) {
            throw new RuntimeException("No password should never be considered 'invalid'", e);
        }
    }

    @Nonnull
    public User createUserWithoutGroup(
            @Nonnull String name,
            @Nullable String displayName,
            @Nullable String email,
            @Nullable char[] password) throws InvalidNameException, NameAlreadyInUseException, IOException, InvalidPasswordException {
        InvalidNameException.ensureValid(name);
        NameAlreadyInUseException.ensureNotPresent(this.users.keySet(), name);

        var user = new User(name, displayName, email, getPasswordHash(password), this);
        this.users.put(name, user);
        return user;
    }

    @Nonnull
    public User updateUser(
            @Nonnull String name,
            @Nullable String displayName,
            @Nullable String email) throws InvalidNameException, NameNotFoundException, IOException, InvalidPasswordException {
        InvalidNameException.ensureValid(name);
        return updateUser(
                name,
                displayName,
                email,
                getUser(name).orElseThrow(() -> new NameNotFoundException(name)).password()
        );
    }

    @Nonnull
    public User updateUser(
            @Nonnull String name,
            @Nullable String displayName,
            @Nullable String email,
            @Nullable char[] password) throws InvalidNameException, NameNotFoundException, IOException, InvalidPasswordException {
        return updateUser(name, displayName, email, getPasswordHash(password));
    }

    @Nonnull
    protected User updateUser(
            @Nonnull String name,
            @Nullable String displayName,
            @Nullable String email,
            @Nullable PasswordHash passwordHash) throws InvalidNameException, NameNotFoundException, IOException {
        InvalidNameException.ensureValid(name);
        NameNotFoundException.ensurePresent(this.users.keySet(), name);

        // for now (without proper persistence), just replace the object
        var user = new User(name, displayName, email, passwordHash, this);
        this.users.put(name, user);
        return user;
    }

    @Nullable
    private PasswordHash getPasswordHash(@Nullable char[] password) throws InvalidPasswordException {
        return password != null
               ? PasswordHash.calculate(InvalidPasswordException.ensureValid(password))
               : null;
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
        try {
            return this.createUserAndGroup(name, null, null, null);
        } catch (InvalidPasswordException e) {
            throw new RuntimeException("No password should never be considered 'invalid'", e);
        }
    }

    @Nonnull
    public User createUserAndGroup(
            @Nonnull String name,
            @Nullable String displayName,
            @Nullable String email,
            @Nullable char[] password) throws InvalidNameException, NameAlreadyInUseException, IOException, InvalidPasswordException {
        var group = Prefix.User.wrap(Prefix.unwrap_or_given(name));
        var user  = this.createUserWithoutGroup(name, displayName, email, password);

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

    /**
     * Updates the {@link PasswordHash} for the {@link User} of the given name. Returns the updated {@link User} instance.
     *
     * @param name     The name of the user to update the password for
     * @param password The new password to set or null to reset
     */
    public void setUserPassword(
            @Nonnull String name,
            @Nullable char[] password) throws IOException, InvalidNameException, NameNotFoundException, InvalidPasswordException {
        InvalidNameException.ensureValid(name);

        var user = this.getUser(name).orElseThrow(() -> new NameNotFoundException(name));

        // for now (without proper persistence), just replace the object
        this.users.put(
                user.name(),
                new User(
                        user.name(),
                        user.displayName(),
                        user.email(),
                        getPasswordHash(password),
                        user.groupAssignmentResolver()
                )
        );
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

    public void deleteUser(@Nonnull String name) throws NameNotFoundException, IOException {
        NameNotFoundException.ensurePresent(this.users.keySet(), name);
        this.users.remove(name);
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
