package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.Role;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

@RestController
public class RoleController {

    private static final Logger LOG = Logger.getLogger(RoleController.class.getSimpleName());

    private final @Nonnull Winslow winslow;

    @Autowired
    public RoleController(Winslow winslow) {
        this.winslow = winslow;
    }

    @ApiOperation(value = "In privileges descendingly ordered list of all available Roles, used e.g. for group memberships")
    @GetMapping("/roles")
    public Role[] getRoles() {
        return Role.values();
    }
}
