package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

@Configuration
public class JsonToMapStringStringConverter implements Converter<String, Map<String, String>> {

    public JsonToMapStringStringConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public Map<String, String> convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new StringStringMapTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class StringStringMapTypeReference extends TypeReference<Map<String, String>> {
    }
}
