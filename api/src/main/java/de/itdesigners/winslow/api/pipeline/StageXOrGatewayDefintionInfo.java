package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public record StageXOrGatewayDefintionInfo(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<String> conditions,
        @Nonnull List<UUID> nextStages) implements StageDefinitionInfo {
}
