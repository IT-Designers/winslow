package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DeserializerUtils {

    public static <T> T deserializeWithDefaultDeserializer(
            @Nonnull JsonNode node,
            @Nonnull DeserializationContext ctxt,
            @Nonnull Class<T> clazz) throws IOException, JsonProcessingException {
        DeserializationConfig config = ctxt.getConfig();
        JavaType              type   = TypeFactory.defaultInstance().constructType(clazz);
        JsonDeserializer<Object> defaultDeserializer = BeanDeserializerFactory.instance.buildBeanDeserializer(
                ctxt,
                type,
                config.introspect(type)
        );

        if (defaultDeserializer instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
        }

        JsonParser treeParser = ctxt.getParser().getCodec().treeAsTokens(node);
        config.initialize(treeParser);

        if (treeParser.getCurrentToken() == null) {
            treeParser.nextToken();
        }

        return (T) defaultDeserializer.deserialize(treeParser, ctxt);
    }
}
