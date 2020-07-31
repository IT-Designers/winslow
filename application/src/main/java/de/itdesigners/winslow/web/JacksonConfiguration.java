package de.itdesigners.winslow.web;

import de.itdesigners.winslow.BaseRepository;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Nonnull;
import java.util.List;

@JsonComponent
@Configuration
@EnableWebMvc
public class JacksonConfiguration implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (var i = 0; i < converters.size(); ++i) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                // replace with custom mapper
                converters.set(i, createConverter());
                return;
            }
        }
        // not present yet, add
        converters.add(createConverter());
    }

    @Nonnull
    private MappingJackson2HttpMessageConverter createConverter() {
        return new MappingJackson2HttpMessageConverter(BaseRepository.defaultObjectMapperModules(
                Jackson2ObjectMapperBuilder
                        .json()
                        .build()
                        .findAndRegisterModules()
        ));
    }

}
