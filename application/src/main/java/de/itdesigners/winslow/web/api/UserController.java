package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
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
    public Stream<User> getUsers(@Nonnull User user) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getUserManager()
                .getUsersPotentiallyIncomplete();
    }
}
