package de.itd.tracking.winslow.auth;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public interface GroupAssignmentResolver {

    boolean canAccessGroup(String user, String group);

    @Nonnull
    Stream<String> getAssignedGroups(String user);
}
