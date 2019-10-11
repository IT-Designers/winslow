package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Env;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@EnableWebMvc
public class PathMapping implements WebMvcConfigurer {

    private static final Integer STATIC_HTML_CACHE_PERIOD = 60 * 60;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (Env.getStaticHtml() != null) {
            var resolver = new PathResourceResolver();
            resolver.setAllowedLocations(new FileSystemResource(Env.getStaticHtml()));

            registry
                    .addResourceHandler("/**")
                    .addResourceLocations("file://" + Env.getStaticHtml())
                    .setCachePeriod(STATIC_HTML_CACHE_PERIOD)
                    .resourceChain(true)
                    .addResolver(resolver);
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // just show the index.html on errors
        registry.addViewController("/error").setViewName("/index.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(Env.getApiPath(), HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
