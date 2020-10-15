package de.itdesigners.winslow.web.webdav;

import io.milton.servlet.MiltonFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import static de.itdesigners.winslow.web.webdav.WebDavController.EXPORT_NAME;

@Configuration
@EnableWebMvc
public class SpringConfiguration implements WebMvcConfigurer {

    private static final String METHOD_PROPFIND = "PROPFIND";
    private static final Set<String> WEBDAV_METHODS = Set.of(
            "HEAD",
            "DELETE",
            "POST",
            "GET",
            "OPTIONS",
            "PATCH",
            "PUT",

            METHOD_PROPFIND,
            "LOCK",
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
                if (METHOD_PROPFIND.equalsIgnoreCase(request.getMethod())) {
                    return false;
                } else if (request.getRequestURI().startsWith("/" + EXPORT_NAME + "/") || request
                        .getRequestURI()
                        .equals("/" + EXPORT_NAME)) {
                    return !WEBDAV_METHODS.contains(request.getMethod());
                } else {
                    return true;
                }
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
        var                            miltonFilter = getMiltonFilter();
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                miltonFilter.init(filterConfig);
            }

            @Override
            public void destroy() {
                miltonFilter.destroy();
            }

            @Override
            public void doFilter(
                    ServletRequest request,
                    ServletResponse response,
                    FilterChain chain) throws IOException, ServletException {
                if (request instanceof HttpServletRequest) {
                    var httpRequest = (HttpServletRequest) request;
                    if (METHOD_PROPFIND.equalsIgnoreCase(httpRequest.getMethod()) || "/webdav".equals(httpRequest.getRequestURI()) || httpRequest
                            .getRequestURI()
                            .startsWith("/webdav/")) {
                        miltonFilter.doFilter(request, response, chain);
                    } else {
                        chain.doFilter(request, response);
                    }
                } else {
                    chain.doFilter(request, response);
                }
            }
        });
        registration.setName("MiltonFilter");
        // registration.addUrlPatterns("/*", "/"+EXPORT_NAME, "/" + EXPORT_NAME + "/*");
        // registration.addUrlPatterns("/*");
        registration.addUrlPatterns("/" + EXPORT_NAME + "/*");
        //        registration.addInitParameter("milton.exclude.paths", "/myExcludedPaths,/moreExcludedPaths");
        registration.addInitParameter(
                "resource.factory.class",
                "io.milton.http.annotated.AnnotationResourceFactory"
        );
        // this does not work in the final jar:
        // registration.addInitParameter("controllerPackagesToScan", getClass().getPackageName());
        registration.addInitParameter("controllerClassNames", WebDavController.class.getName());
        registration.addInitParameter("milton.configurator", MiltonConfiguration.class.getName());
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        return registration;
    }

    public Filter getMiltonFilter() {
        return new MiltonFilter();
    }
}
