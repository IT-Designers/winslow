package de.itdesigners.winslow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

public interface StageGatewayDefinitionInfo extends StageDefinitionInfo {
    @JsonProperty
    @Nonnull GatewaySubType gatewaySubType();
}
