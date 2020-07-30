package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

@Configuration
public class JsonToMapStringRangedValueConverter implements Converter<String, Map<String, RangedValue>> {

    public JsonToMapStringRangedValueConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public Map<String, RangedValue> convert(@Nonnull String s) {
        try {
            return BaseRepository
                    .defaultObjectMapperModules(new ObjectMapper())
                    .readValue(s, new StringRangedValueTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class StringRangedValueTypeReference extends TypeReference<Map<String, RangedValue>> {
    }
}
