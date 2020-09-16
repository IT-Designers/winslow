package de.itdesigners.winslow.web.websocket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;

public class PermissionCheckedPayload implements PrincipalPermissionChecker {
    private final @Nonnull PrincipalPermissionChecker checker;
    private final @Nonnull Object                     value;

    public PermissionCheckedPayload(
            @Nonnull PrincipalPermissionChecker checker,
            @Nonnull Object value) {
        this.checker = checker;
        this.value   = value;
    }

    @Nonnull
    public PrincipalPermissionChecker getChecker() {
        return checker;
    }

    @Nonnull
    public Object getValue() {
        return value;
    }

    @Override
    public boolean allowed(@Nullable Principal principal) {
        return getChecker().allowed(principal);
    }
}
