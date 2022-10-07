package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.auth.GroupInfo;
import de.itdesigners.winslow.auth.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class GroupInfoConverter {

    @Nonnull
    public static GroupInfo from(@Nonnull Group group) {
        return new GroupInfo(
                group.name(),
                List.copyOf(group.members())
        );
    }
}
