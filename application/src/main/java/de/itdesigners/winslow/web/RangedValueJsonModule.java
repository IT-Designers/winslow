package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.itdesigners.winslow.api.pipeline.RangeWithStepSize;
import de.itdesigners.winslow.api.pipeline.RangedList;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.config.DeserializerUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class RangedValueJsonModule extends SimpleModule {

    public static final @Nonnull Map<Class<? extends RangedValue>, String> MAPPING = Map.of(
            RangeWithStepSize.class, "DiscreteSteps",
            RangedList.class, "List"
    );

    public RangedValueJsonModule() {
        super(RangedValueJsonModule.class.getSimpleName());


        addDeserializer(RangedValue.class, new RangedValueJsonModule.Deserialize());
        addSerializer(RangedValue.class, new RangedValueJsonModule.Serialize());

        for (var key : MAPPING.keySet()) {
            addSerializer(key, new RangedValueJsonModule.Serialize());
        }
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
    }

    public static class Deserialize extends JsonDeserializer<RangedValue> {
        @Override
        public RangedValue deserialize(
                JsonParser p,
                DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);

            for (var entry : MAPPING.entrySet()) {
                if (node.has(entry.getValue())) {
                    return DeserializerUtils.deserializeWithDefaultDeserializer(
                            node.get(entry.getValue()),
                            ctxt,
                            entry.getKey()
                    );
                }
            }

            return DeserializerUtils.deserializeWithDefaultDeserializer(
                    node,
                    ctxt,
                    RangeWithStepSize.class
            );

        }
    }

    public static class Serialize extends JsonSerializer<RangedValue> {
        @Override
        public void serialize(
                RangedValue value,
                JsonGenerator gen,
                SerializerProvider serializers) throws IOException {
            DeserializerUtils.serializeWithDefaultSerializer(
                    value,
                    gen,
                    serializers,
                    Optional.ofNullable(MAPPING.get(value.getClass()))
                            .orElseThrow(() -> new IOException("Unexpected RangedValue type " + value.getClass()))
            );
        }
    }
}
