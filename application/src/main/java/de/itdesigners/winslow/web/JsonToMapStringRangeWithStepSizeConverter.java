package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itdesigners.winslow.api.pipeline.RangeWithStepSize;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

@Configuration
public class JsonToMapStringRangeWithStepSizeConverter implements Converter<String, Map<String, RangeWithStepSize>> {

    public JsonToMapStringRangeWithStepSizeConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public Map<String, RangeWithStepSize> convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new StringRangeWithStepSizeMapTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class StringRangeWithStepSizeMapTypeReference extends TypeReference<Map<String, RangeWithStepSize>> {
    }
}
