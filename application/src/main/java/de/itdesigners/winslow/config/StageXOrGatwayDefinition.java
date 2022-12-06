package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public record StageXOrGatwayDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull String[] conditions,
        @Nonnull List<UUID> nextStages) implements StageDefinition {

}
