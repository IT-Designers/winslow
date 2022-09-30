package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public interface GroupAssignmentResolver {

    boolean isPartOfGroup(@Nonnull String user, @Nonnull String group);

    @Nonnull
    Stream<Group> getAssignedGroups(@Nonnull String user);

    @Nonnull
    Optional<Group> getGroup(@Nonnull String name);
}
