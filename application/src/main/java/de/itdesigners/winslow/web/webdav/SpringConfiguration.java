package de.itdesigners.winslow.web.webdav;

import io.milton.servlet.MiltonFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Set;

import static de.itdesigners.winslow.web.webdav.WebDavController.EXPORT_NAME;

@Configuration
@EnableWebMvc
public class SpringConfiguration implements WebMvcConfigurer {

    private static final Set<String> WEBDAV_METHODS = Set.of(
            "HEAD",
            "DELETE",
            "POST",
            "GET",
            "OPTIONS",
            "PATCH",
            "PUT",

            "PROPFIND",
            // "LOCK",
            "REPORT",
            "PROPPATCH",
            "MKCOL",
            "MOVE"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AsyncHandlerInterceptor() {
            @Override
            public boolean preHandle(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler) throws Exception {
                return !WEBDAV_METHODS.contains(request.getMethod());
            }

            @Override
            public void afterConcurrentHandlingStarted(
                    HttpServletRequest request,
                    HttpServletResponse response, Object handler) throws Exception {

            }
        });
    }

    @Bean
    public StrictHttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(new ArrayList<>(WEBDAV_METHODS));
        return firewall;
    }

    @Bean
    public FilterRegistrationBean<Filter> someFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(getMiltonFilter());
        registration.setName("MiltonFilter");
        registration.addUrlPatterns("/" + EXPORT_NAME + "/*");
        //        registration.addInitParameter("milton.exclude.paths", "/myExcludedPaths,/moreExcludedPaths");
        registration.addInitParameter(
                "resource.factory.class",
                "io.milton.http.annotated.AnnotationResourceFactory"
        );
        registration.addInitParameter("controllerPackagesToScan", getClass().getPackageName());
        registration.addInitParameter("milton.configurator", MiltonConfiguration.class.getName());
        registration.setOrder(1);
        return registration;
    }

    public Filter getMiltonFilter() {
        return new MiltonFilter();
    }
}
