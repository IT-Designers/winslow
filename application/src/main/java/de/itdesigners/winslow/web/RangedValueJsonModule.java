package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.itdesigners.winslow.api.pipeline.RangeWithStepSize;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.config.DeserializerUtils;

import java.io.IOException;

public class RangedValueJsonModule extends SimpleModule {

    public static final String TYPE_RANGE_WITH_STEP_SIZE = "DiscreteSteps";

    public RangedValueJsonModule() {
        super(RangedValueJsonModule.class.getSimpleName());

        addDeserializer(RangedValue.class, new RangedValueJsonModule.Deserialize());
        addSerializer(RangedValue.class, new RangedValueJsonModule.Serialize());
        addSerializer(RangeWithStepSize.class, new RangedValueJsonModule.Serialize());
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

            if (node.has(TYPE_RANGE_WITH_STEP_SIZE)) {
                return DeserializerUtils.deserializeWithDefaultDeserializer(
                        node.get(TYPE_RANGE_WITH_STEP_SIZE),
                        ctxt,
                        RangeWithStepSize.class
                );
            } else {
                return DeserializerUtils.deserializeWithDefaultDeserializer(
                        node,
                        ctxt,
                        RangeWithStepSize.class
                );
            }
        }
    }

    public static class Serialize extends JsonSerializer<RangedValue> {
        @Override
        public void serialize(
                RangedValue value,
                JsonGenerator gen,
                SerializerProvider serializers) throws IOException {
            String type;

            if (value instanceof RangeWithStepSize) {
                type = TYPE_RANGE_WITH_STEP_SIZE;
            } else {
                throw new IOException("Unexpected RangedValue type " + value.getClass());
            }

            DeserializerUtils.serializeWithDefaultSerializer(
                    value,
                    gen,
                    serializers,
                    type
            );
        }
    }
}
