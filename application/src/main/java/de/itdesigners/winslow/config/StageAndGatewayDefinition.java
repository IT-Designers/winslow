package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.itdesigners.winslow.config.StageWorkerDefinition.idFromName;

public record StageAndGatewayDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<UUID> nextStages) implements StageDefinition {

    public StageAndGatewayDefinition(
            @Nullable UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable List<UUID> nextStages) {
        this.id          = id != null ? id : idFromName(name);
        this.name        = name;
        this.description = description != null ? description : "";
        this.nextStages  = nextStages != null ? nextStages : Collections.emptyList();
        this.check();
    }

}
