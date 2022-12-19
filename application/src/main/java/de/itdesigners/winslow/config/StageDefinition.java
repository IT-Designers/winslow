package de.itdesigners.winslow.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nonnull;
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
    @Nonnull UUID id();
    @Nonnull String name();
    @Nonnull List<UUID> nextStages();

    default void check() throws RuntimeException {
        Objects.requireNonNull(id(), "The id of a stage must be set");
        Objects.requireNonNull(name(), "The name of a stage must be set");
        if (name().isBlank()) {
            throw new IllegalArgumentException("The name of a stage must not be blank");
        }
    }

    default Map<String, String> environment() {
        return Collections.emptyMap();
    }

}
