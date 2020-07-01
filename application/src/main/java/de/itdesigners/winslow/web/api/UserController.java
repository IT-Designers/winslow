package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class UserController {

    @Autowired
    public UserController(Winslow winslow) {

    }

    @GetMapping("/users/self/name")
    public Optional<String> getSelfName(User user) {
        return Optional.ofNullable(user).map(User::getName);
    }
}
