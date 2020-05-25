package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;

@Configuration
public class JsonToImageInfoConverter implements Converter<String, ImageInfo> {

    public JsonToImageInfoConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public ImageInfo convert(@Nonnull String s) {
        try {
            var om = new ObjectMapper();
            om.findAndRegisterModules();
            return om.readValue(s, new ImageInfoTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class ImageInfoTypeReference extends TypeReference<ImageInfo> {
    }
}
