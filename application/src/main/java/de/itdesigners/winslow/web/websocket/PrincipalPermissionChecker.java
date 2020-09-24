package de.itdesigners.winslow.web.websocket;

import javax.annotation.Nullable;

public interface PrincipalPermissionChecker {
    boolean allowed(@Nullable String user);
}
