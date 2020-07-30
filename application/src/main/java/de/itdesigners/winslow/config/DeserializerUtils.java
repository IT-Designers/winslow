package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public static <T> void serializeWithDefaultSerializer(
            @Nonnull T value,
            @Nonnull JsonGenerator gen,
            @Nonnull SerializerProvider provider) throws IOException, JsonProcessingException {
        serializeWithDefaultSerializer(value, gen, provider, null);
    }

    public static <T> void serializeWithDefaultSerializer(
            @Nonnull T value,
            @Nonnull JsonGenerator gen,
            @Nonnull SerializerProvider provider,
            @Nullable String variantName) throws IOException, JsonProcessingException {
        JavaType        javaType = provider.constructType(value.getClass());
        BeanDescription beanDesc = provider.getConfig().introspect(javaType);
        JsonSerializer<Object> serializer = BeanSerializerFactory.instance.findBeanOrAddOnSerializer(
                provider,
                javaType,
                beanDesc,
                false
        );

        gen.writeStartObject();

        if (variantName != null) {
            gen.writeFieldName(variantName);
            gen.writeStartObject();
            serializer.unwrappingSerializer(null).serialize(value, gen, provider);
            gen.writeEndObject();
        } else {
            serializer.unwrappingSerializer(null).serialize(value, gen, provider);
        }

        gen.writeEndObject();
    }

    public interface PrependObjectFieldsCallback {
        void prepend(@Nonnull JsonGenerator generator) throws IOException;
    }
}
