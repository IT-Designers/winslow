package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface GroupAssignmentResolver {

    boolean isPartOfGroup(@Nonnull String user, @Nonnull String group);

    @Nonnull
    List<Group> getAssignedGroups(@Nonnull String user);

    @Nonnull
    Optional<Group> getGroup(@Nonnull String name);
}
