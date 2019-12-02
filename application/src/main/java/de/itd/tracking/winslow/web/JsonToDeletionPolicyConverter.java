package de.itd.tracking.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itd.tracking.winslow.pipeline.DeletionPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Configuration
public class JsonToDeletionPolicyConverter implements Converter<String, DeletionPolicy> {

    public JsonToDeletionPolicyConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public DeletionPolicy convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new StringListTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class StringListTypeReference extends TypeReference<DeletionPolicy> {
    }
}
