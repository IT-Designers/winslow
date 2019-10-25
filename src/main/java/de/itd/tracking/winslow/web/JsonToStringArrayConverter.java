package de.itd.tracking.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.regex.Pattern;

@Configuration
public class JsonToStringArrayConverter implements Converter<String, String[]> {

    private static final Pattern SPLIT = Pattern.compile(",");

    public JsonToStringArrayConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public String[] convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new StringTypeReference());
        } catch (IOException e) {
            return SPLIT.split(s);
        }
    }

    private static class StringTypeReference extends TypeReference<String[]> {
    }
}
