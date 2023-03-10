package de.itdesigners.winslow.api.node;

import de.itdesigners.winslow.api.settings.ResourceLimitation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * The resource limit of a node can be configured in two modi:
 *
 * <ul>
 *     <li>
 *         If {@link #freeForAll} is configured, the configured node that can be used by anybody without being mentioned
 *         explicitly. A global upper resource usage limit may be applied anyway.
 *     </li>
 *     <li>
 *         If {@link #freeForAll} is not configured, limitations apply as configured individually and no access is
 *         granted without the group or user being mentioned explicitly (except privileged users).
 *     </li>
 * </ul>
 */
public record NodeResourceUsageConfiguration(
        boolean freeForAll,
        @Nonnull Optional<ResourceLimitation> globalLimit,
        @Nonnull List<GroupResourceLimitEntry> groupLimits
) {
}

