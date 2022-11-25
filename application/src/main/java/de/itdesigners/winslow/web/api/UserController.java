package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.UserInfo;
import de.itdesigners.winslow.auth.*;
import de.itdesigners.winslow.web.UserInfoConverter;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@RestController
public class UserController {

    private static final Logger LOG = Logger.getLogger(UserController.class.getSimpleName());

    private final @Nonnull Winslow winslow;

    @Autowired
    public UserController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/users/self/name")
    public Optional<String> getSelfName(@Nullable User user) {
        return Optional.ofNullable(user).map(User::name);
    }

    @ApiOperation(value = "A potentially incomplete list of known Users")
    @GetMapping("/users")
    public Stream<UserInfo> getUsers(@Nullable User user) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getUserManager()
                .getUsersPotentiallyIncomplete()
                .map(UserInfoConverter::from);
    }

    @GetMapping("/users/{name}/available")
    public ResponseEntity<String> getUserNameAvailable(
            @Nullable User user,
            @PathVariable("name") String name) {
        try {
            InvalidNameException.ensureValid(name);
            if (winslow
                    .getUserManager()
                    .getUser(name)
                    .isPresent()) {
                return new ResponseEntity<>("Name already taken.", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (InvalidNameException e) {
            return new ResponseEntity<>(
                    "Invalid name. Name must contain only: " + InvalidNameException.REGEX_PATTERN_DESCRIPTION + " and not exceed " + InvalidNameException.MAX_LENGTH + " characters",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/users/{name}")
    public Optional<UserInfo> getUser(
            @Nullable User user,
            @PathVariable("name") String name) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getUserManager()
                .getUser(name)
                .filter(userToBeLookedAt -> isAllowedToSeeUser(user, userToBeLookedAt))
                .map(UserInfoConverter::from);
    }

    @PostMapping("/users")
    public UserInfo createUser(
            @Nullable User user,
            @RequestBody UserInfo newUser) {
        try {
            ensure(isAllowedToCreateUser(user));

            return UserInfoConverter.from(
                    winslow.getUserManager().createUserWithoutGroup(
                            newUser.name(),
                            newUser.displayName(),
                            newUser.email(),
                            newUser.password()
                    )
            );
        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NameAlreadyInUseException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Name already in use", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create user because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/users")
    public UserInfo updateUser(
            @Nullable User user,
            @RequestBody UserInfo update) {
        try {
            ensure(isAllowedToModifyUser(user, update.name()));

            if (update.password() == null) {
                return UserInfoConverter.from(
                        winslow.getUserManager().updateUser(
                                update.name(),
                                update.displayName(),
                                update.email(),
                                update.active()
                        )
                );
            } else {
                return UserInfoConverter.from(
                        winslow.getUserManager().updateUser(
                                update.name(),
                                update.displayName(),
                                update.email(),
                                update.active(),
                                update.password()
                        )
                );
            }
        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create user because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/users/{name}/password")
    public void setPassword(
            @Nullable User user,
            @Nonnull @PathVariable("name") String name,
            // https://stackoverflow.com/a/45250557
            @RequestBody String password) {
        try {
            ensure(isAllowedToModifyUser(user, name));
            winslow.getUserManager().setPassword(name, password.toCharArray());
        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create user because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "Removes the user password", notes = "Requires super privileges. This might cause the user to no longer being able to login.")
    @DeleteMapping("/users/{name}/password")
    public void deletePassword(
            @Nullable User user,
            @Nonnull @PathVariable("name") String name) {
        try {
            ensure(isAllowedToDeleteUserPassword(user));
            winslow.getUserManager().deletePassword(name);
        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create user because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/users/{name}/super-privileges")
    public Optional<Boolean> hasSuperPrivileges(
            @Nullable User user,
            @PathVariable("name") String name) {
        return winslow
                .getUserManager()
                .getUser(name)
                .filter(u -> isAllowedToSeeUser(user, u))
                .map(User::hasSuperPrivileges);
    }

    @DeleteMapping("/users/{name}")
    public void deleteUser(
            @Nullable User user,
            @PathVariable("name") String name) {
        try {
            ensure(isAllowedToDeleteUser(user));
            winslow.getUserManager().deleteUser(name);
        } catch (NameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create user because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private void ensure(boolean accessGranted) throws ResponseStatusException {
        if (!accessGranted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isAllowedToModifyUser(@Nullable User user, @Nonnull String toBeModified) {
        // only privileged users and the user itself can modify a user
        return user != null && (user.hasSuperPrivileges() || toBeModified.equals(user.name()));
    }

    private boolean isAllowedToDeleteUserPassword(@Nullable User user) {
        // only privileged users can unset passwords
        return user != null && user.hasSuperPrivileges();
    }

    private boolean isAllowedToDeleteUser(@Nullable User user) {
        // only privileged users can delete users
        return user != null && user.hasSuperPrivileges();
    }

    private boolean isAllowedToCreateUser(@Nullable User user) {
        // only privileged users can create new users
        return user != null && user.hasSuperPrivileges();
    }

    private boolean isAllowedToSeeUser(@Nullable User user, @Nonnull User toBeLookedAt) {
        // ordered in ascending query complexity
        return user != null && (user.hasSuperPrivileges() || toBeLookedAt.name().equals(user.name()));
    }
}
