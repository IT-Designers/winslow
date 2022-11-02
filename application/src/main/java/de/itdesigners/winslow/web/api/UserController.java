package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.UserInfo;
import de.itdesigners.winslow.auth.InvalidNameException;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.web.UserInfoConverter;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
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
    public Optional<String> getSelfName(User user) {
        return Optional.ofNullable(user).map(User::name);
    }

    @ApiOperation(value = "A potentially incomplete list of known Users")
    @GetMapping("/users")
    public Stream<UserInfo> getUsers(@Nonnull User user) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getUserManager()
                .getUsersPotentiallyIncomplete()
                .map(UserInfoConverter::from);
    }

    @GetMapping("/users/{name}/available")
    public ResponseEntity<String> getUserNameAvailable(
            @Nonnull User user,
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
                    "Invalid name. Name must contain only: " + InvalidNameException.REGEX_PATTERN_DESCRIPTION + " and not exceed "+ InvalidNameException.MAX_LENGTH +" characters",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/users/{name}")
    public Optional<UserInfo> getUser(
            @Nonnull User user,
            @PathVariable("name") String name) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getUserManager()
                .getUser(name)
                .filter(userToBeLookedAt -> isAllowedToSeeGroup(user, userToBeLookedAt))
                .map(UserInfoConverter::from);
    }

    private boolean isAllowedToSeeGroup(@Nullable User user, @Nonnull User toBeLookedAt) {
        // ordered in ascending query complexity
        return user != null && (user.hasSuperPrivileges() || toBeLookedAt.name().equals(user.name()));
    }
}
