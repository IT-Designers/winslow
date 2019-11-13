package de.itd.tracking.winslow.auth;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public interface GroupAssignmentResolver {

    boolean canAccessGroup(@Nonnull String user, @Nonnull String group);

    @Nonnull
    Stream<String> getAssignedGroups(@Nonnull String user);

    @Nonnull
    Optional<Group> getGroup(@Nonnull String name);
}
