package de.itdesigners.winslow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StageWorkerDefinitionInfo.class, name = "Worker"),
        @JsonSubTypes.Type(value = StageXOrGatewayDefinitionInfo.class, name = "XorGateway"),
        @JsonSubTypes.Type(value = StageAndGatewayDefinitionInfo.class, name = "AndGateway"),
})
public interface StageDefinitionInfo {
    @JsonProperty
    @Nonnull UUID id();

    @JsonProperty
    @Nonnull String name();

    @JsonProperty
    @Nonnull List<UUID> nextStages();
}
