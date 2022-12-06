package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public record StageAndGatewayDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<UUID> nextStages) implements StageDefinition {
}
