package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;

@Configuration
public class JsonToResourceInfoConverter implements Converter<String, ResourceInfo> {

    public JsonToResourceInfoConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public ResourceInfo convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new ResourceInfoTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class ResourceInfoTypeReference extends TypeReference<ResourceInfo> {
    }
}
