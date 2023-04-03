package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ACL {

    public static boolean canUserAccess(@Nonnull User user, @Nonnull List<Link> groups) {
        return canUserAccess(user, groups, null);
    }

    public static boolean canUserAccess(@Nonnull User user, @Nonnull List<Link> groups, @Nullable String owner) {
        return canUserFulfillAnyRole(user, groups, owner, Role.values());
    }

    public static boolean canUserManage(@Nonnull User user, @Nonnull List<Link> groups) {
        return canUserManage(user, groups, null);
    }

    public static boolean canUserManage(@Nonnull User user, @Nonnull List<Link> groups, @Nullable String owner) {
        return canUserFulfillAnyRole(user, groups, owner, Role.OWNER);
    }

    public static boolean canUserFulfillAnyRole(@Nonnull User user, @Nonnull List<Link> groups, @Nullable String owner, @Nonnull Role...roles) {
        if (user.hasSuperPrivileges()) {
            return true;
        } else if (Objects.equals(user.name(), owner)) {
            return true;
        } else {
            return hasGroupMembership(user, groups, roles);
        }
    }

    public static boolean hasGroupMembership(@Nonnull User user, @Nonnull List<Link> groups, @Nonnull Role... roles) {
        return groups
                .stream()
                .anyMatch(link -> Arrays.stream(roles).anyMatch(m -> link.role() == m)
                        && user
                        .getGroups()
                        .stream()
                        .anyMatch(g -> Objects.equals(link.name(), g.name()))
                );
    }
}
