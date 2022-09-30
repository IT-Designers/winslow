package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.auth.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@EnableWebMvc
@Configuration
public class UserResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    private final UserRepository users;

    public UserResolver(Winslow winslow) {
        this.users = winslow.getUserRepository();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(User.class);
    }

    @Nullable
    @Override
    public User resolveArgument(
            MethodParameter methodParameter,
            ModelAndViewContainer modelAndViewContainer,
            NativeWebRequest nativeWebRequest,
            WebDataBinderFactory webDataBinderFactory) throws Exception {
        return Optional
                .ofNullable(nativeWebRequest.getRemoteUser())
                .or(Env::getDevUser)
                .flatMap(users::getUserOrCreateAuthenticated)
                .orElse(null);
    }
}

