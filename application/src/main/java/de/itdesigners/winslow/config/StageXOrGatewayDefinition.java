package de.itdesigners.winslow.config;

import de.itdesigners.winslow.api.pipeline.GatewaySubType;
import de.itdesigners.winslow.api.pipeline.StageGatewayDefinitionInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record StageXOrGatewayDefinition(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<String> conditions,
        @Nonnull List<UUID> nextStages,
        @Nonnull GatewaySubType gatewaySubType) implements StageDefinition, StageGatewayDefinitionInfo {

    public StageXOrGatewayDefinition(
            @Nullable UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable List<String> conditions,
            @Nullable List<UUID> nextStages,
            @Nonnull GatewaySubType gatewaySubType) {
        this.id             = id != null ? id : StageDefinition.idFromName(name);
        this.name           = name;
        this.description    = description != null ? description : "";
        this.conditions     = conditions != null ? conditions : Collections.emptyList();
        this.nextStages     = nextStages != null ? nextStages : Collections.emptyList();
        this.gatewaySubType = gatewaySubType;
        this.check();
    }

}
