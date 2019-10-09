package de.itd.tracking.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Configuration
public class JsonToListStringConverter implements Converter<String, List<String>> {

    public JsonToListStringConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public List<String> convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
