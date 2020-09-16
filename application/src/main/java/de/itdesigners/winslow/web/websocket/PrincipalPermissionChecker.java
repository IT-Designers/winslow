package de.itdesigners.winslow.web.websocket;

import javax.annotation.Nullable;
import java.security.Principal;

public interface PrincipalPermissionChecker {
    boolean allowed(@Nullable Principal principal);
}
