package de.itdesigners.winslow.web;

import de.itdesigners.winslow.auth.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@ComponentScan(basePackageClasses = WebApi.class)
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .genericModelSubstitutes(Optional.class)
                .genericModelSubstitutes(Stream.class)
                .genericModelSubstitutes(ResponseEntity.class)
                //.directModelSubstitute(InputStreamResource.class, InputStream.class)
                .ignoredParameterTypes(User.class)
                .apiInfo(apiEndpointsInfo());
    }

    private ApiInfo apiEndpointsInfo() {
        return new ApiInfoBuilder().title("Winslow REST API").description("REST API to interface with Winslow").build();
    }
}
