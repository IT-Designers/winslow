package de.itdesigners.winslow.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itdesigners.winslow.pipeline.WorkspaceConfiguration.WorkspaceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.Nonnull;
import java.io.IOException;

@Configuration
public class JsonToWorkspaceModeConverter implements Converter<String, WorkspaceMode> {

    public JsonToWorkspaceModeConverter(ConverterRegistry registry) {
        registry.addConverter(this);
    }

    @Override
    public WorkspaceMode convert(@Nonnull String s) {
        try {
            return new ObjectMapper().readValue(s, new WorkspaceModeTypeReference());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class WorkspaceModeTypeReference extends TypeReference<WorkspaceMode> {
    }
}
