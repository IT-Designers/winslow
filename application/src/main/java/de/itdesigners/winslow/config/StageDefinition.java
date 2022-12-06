package de.itdesigners.winslow.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT
        //include = JsonTypeInfo.As.PROPERTY,
        //property = "type"

)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StageWorkerDefinition.class, name = "Worker"),
        @JsonSubTypes.Type(value = StageXOrGatwayDefinition.class, name = "XorGateway"),
        @JsonSubTypes.Type(value = StageAndGatewayDefinition.class, name = "AndGateway"),
})

public interface StageDefinition {
    UUID id();
    String name();
    List<UUID> nextStages();

    default void check() {
        Objects.requireNonNull(name(), "The name of a stage must be set");
    }

    default Map<String, String> environment() {
        return Collections.emptyMap();
    }

}
