package de.itdesigners.winslow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
     //   include = JsonTypeInfo.As.WRAPPER_OBJECT
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"

)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StageWorkerDefinitionInfo.class, name = "Worker"),
        @JsonSubTypes.Type(value = StageXOrGatewayDefintionInfo.class, name = "XorGateway"),
        @JsonSubTypes.Type(value = StageAndGatewayDefinitionInfo.class, name = "AndGateway"),
})
public interface StageDefinitionInfo {
    UUID id();
    String name();
    List<UUID> nextStages();
}
