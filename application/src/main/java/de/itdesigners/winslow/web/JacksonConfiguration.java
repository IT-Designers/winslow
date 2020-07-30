package de.itdesigners.winslow.web;

import de.itdesigners.winslow.BaseRepository;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@JsonComponent
@Configuration
@EnableWebMvc
public class JacksonConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(BaseRepository.defaultObjectMapperModules(
                Jackson2ObjectMapperBuilder
                        .json()
                        .build()
                        .findAndRegisterModules()
        )));
    }

}
