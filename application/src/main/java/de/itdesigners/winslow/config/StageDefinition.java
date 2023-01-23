package de.itdesigners.winslow.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;

import javax.annotation.Nonnull;
import java.beans.Transient;
import java.util.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT
        //include = JsonTypeInfo.As.PROPERTY,
        //property = "type"

)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StageWorkerDefinition.class, name = "Worker"),
        @JsonSubTypes.Type(value = StageXOrGatewayDefinition.class, name = "XorGateway"),
        @JsonSubTypes.Type(value = StageAndGatewayDefinition.class, name = "AndGateway"),
})

public interface StageDefinition extends StageDefinitionInfo {
    @Nonnull
    @Override
    UUID id();

    @Nonnull
    @Override
    String name();

    @Nonnull
    String description();

    @Nonnull
    @Override
    List<UUID> nextStages();

    default void check() throws RuntimeException {
        Objects.requireNonNull(id(), "The id of a stage must be set");
        Objects.requireNonNull(name(), "The name of a stage must be set");
        Objects.requireNonNull(description(), "The description of a stage must be set");
        Objects.requireNonNull(nextStages(), "The next stages of a stage must be set");

        if (name().isBlank()) {
            throw new IllegalArgumentException("The name of a stage must not be blank");
        }
    }

    @Nonnull
    default Map<String, String> environment() {
        return Collections.emptyMap();
    }

    /**
     * This functions as a legacy import tool, to generate reproducable {@link UUID}s for matching by name.
     *
     * @param name The name to generate the {@link UUID} for
     * @return A {@link UUID} soley based on the given name
     */
    @Nonnull
    @Transient
    static UUID idFromName(@Nonnull String name) {
        return new UUID(name.length(), ((long) name.hashCode()) << 32 | ((long) name.hashCode()));
    }
}
