package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public record StageAndGatewayDefinitionInfo(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<UUID> nextStages,
        @Nonnull GatewaySubType gatewaySubType) implements StageGatewayDefinitionInfo {
}
