package de.itdesigners.winslow.node;

import de.itdesigners.winslow.api.node.GroupResourceLimitEntry;
import de.itdesigners.winslow.api.node.NodeResourceUsageConfiguration;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.auth.Group;
import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * See {@link NodeResourceUsageConfiguration}
 */
public record NodeResourceLimitFinder(@Nonnull NodeResourceUsageConfiguration configuration) {

    public static final @Nonnull ResourceLimitation             LIMIT_ZERO          = new ResourceLimitation(
            0L, 0L, 0L
    );
    public static final @Nonnull NodeResourceUsageConfiguration ONLY_FOR_PRIVILEGED = new NodeResourceUsageConfiguration(
            false, Optional.empty(), Collections.emptyList()
    );

    /**
     * @return The fallback value for scenarios without a recognized {@link User}
     */
    @Nonnull
    public Optional<ResourceLimitation> getFallback() {
        if (configuration.freeForAll()) {
            return configuration.globalLimit();
        } else {
            return Optional.of(LIMIT_ZERO);
        }
    }

    @Nonnull
    public Optional<ResourceLimitation> getAppliedLimit(@Nonnull User user) {
        if (user.hasSuperPrivileges()) {
            return Optional.empty();
        } else {
            return user
                    .getGroups()
                    .stream()
                    .map(this::getAppliedLimit)
                    .reduce(Optional.of(LIMIT_ZERO), (a, b) -> {
                        if (a.isPresent() && b.isPresent()) {
                            return Optional.of(a.get().max(b.get()));
                        } else {
                            // at least for one group there is unrestricted access
                            return Optional.empty();
                        }
                    });
        }
    }

    @Nonnull
    public Optional<ResourceLimitation> getAppliedLimit(@Nonnull Group group) {
        if (group.isSuperGroup()) {
            return Optional.empty();
        } else if (configuration.freeForAll()) {
            return configuration.globalLimit();
        } else {
            return Optional.of(
                    configuration
                            .groupLimits()
                            .stream()
                            .filter(e -> Objects.equals(e.name(), group.name()))
                            .map(GroupResourceLimitEntry::resourceLimitation)
                            .findFirst()
                            // no entry means no access
                            .orElse(LIMIT_ZERO)
            );
        }
    }
}
