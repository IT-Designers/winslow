package de.itdesigners.winslow.api.node;

import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.settings.ResourceLimitation;

import javax.annotation.Nonnull;

public record GroupResourceLimitEntry(
        @Nonnull String name,
        @Nonnull Role role,
        @Nonnull ResourceLimitation resourceLimitation
) {
}
