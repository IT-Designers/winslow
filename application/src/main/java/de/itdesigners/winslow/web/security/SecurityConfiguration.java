package de.itdesigners.winslow.web.security;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOG = Logger.getLogger(SecurityConfiguration.class.getSimpleName());

    private @Value("${winslow.ldap.user.search-base:#{null}}")             String userSearchBase;
    private @Value("${winslow.ldap.user.search-filter:#{\"(uid={0})\"}}")  String userSearchFilter;
    private @Value("${winslow.ldap.group.search-base:#{null}}")            String groupSearchBase;
    private @Value("${winslow.ldap.group.search-filter:#{\"(gid={0})\"}}") String groupSearchFilter;
    private @Value("${winslow.ldap.url:#{null}}")                          String ldapUrl;
    private @Value("${winslow.ldap.manager.dn:#{null}}")                   String managerDn;
    private @Value("${winslow.ldap.manager.password:#{null}}")             String managerPassword;

    private @Autowired Winslow winslow;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        configureHeaders(http);
        configureCsrfToken(http);
        configureAuthorization(http);
        configureTransport(http);
        configureAfterFilter(http);

        return http.build();
    }

    private void configureHeaders(HttpSecurity http) throws Exception {
        http.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
    }

    private void configureAfterFilter(HttpSecurity http) {
        http.addFilterAfter(this::invalidateSessionWhenUserIsInactive, AuthorizationFilter.class);
        http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    }


    private void configureTransport(HttpSecurity http) throws Exception {
        if (Env.requireSecure()) {
            http.requiresChannel(channelRequestMatcherRegistry -> channelRequestMatcherRegistry
                    .anyRequest()
                    .requiresSecure());
        }
    }

    private void configureAuthorization(HttpSecurity http) throws Exception {
        if (Env.isProdEnv() || Env.isAuthMethodSet()) {
            LOG.info("Authorization: Requiring login to access Winslow");
            http
                    .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry
                            .requestMatchers(Env.getApiNoAuthPath() + "**")
                            .permitAll()
                            .anyRequest().authenticated()
                    )
                    .formLogin(Customizer.withDefaults())
                    .httpBasic(Customizer.withDefaults());
        } else {
            LOG.info("Authorization: No login required to access Winslow");
        }
    }

    private void configureCsrfToken(HttpSecurity http) throws Exception {

        if (Env.isProdEnv()) {
            http
                    .csrf((csrf) -> {
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                        csrf.ignoringRequestMatchers(Env.getWebsocketPath() + "**");
                        csrf.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler());
                    });
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

    }


    private void invalidateSessionWhenUserIsInactive(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest sr) {
            var user = Optional.of(sr).filter(r -> r.getUserPrincipal() != null && r
                    .getUserPrincipal()
                    .getName() != null).flatMap(r -> winslow
                    .getUserManager()
                    .getUser(r.getUserPrincipal().getName()));

            if (user.isPresent() && !user.get().active()) {
                sr.getSession().invalidate();
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }


    @Autowired
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (Env.isLdapAuthEnabled()) {
            LOG.info("Authentication: Using LDAP authentication");
            auth.ldapAuthentication().userSearchBase(userSearchBase).userSearchFilter(userSearchFilter).groupSearchBase(
                    groupSearchBase).groupSearchFilter(groupSearchFilter).contextSource().url(ldapUrl).managerDn(
                    managerDn).managerPassword(managerPassword);
        } else if (Env.isLocalAuthEnabled()) {
            LOG.info("Authentication: Using built-in authentication");
            auth.authenticationProvider(new AuthenticationProvider() {
                @Override
                public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                    return Optional
                            .ofNullable(authentication.getCredentials())
                            .filter(String.class::isInstance)
                            .map(Object::toString)
                            .flatMap(pw -> winslow
                                    .getUserManager()
                                    .getUser(authentication.getName().trim())
                                    .filter(user -> user.active() && user.password() != null && user
                                            .password()
                                            .isPasswordCorrect(pw))
                                    .map(user -> new UsernamePasswordAuthenticationToken(
                                            user.name(),
                                            null,
                                            // no need to store the password ... right?
                                            new ArrayList<>()
                                    )))
                            .orElseThrow(() -> new BadCredentialsException("Invalid login"));
                }


                @Override
                public boolean supports(Class<?> aClass) {
                    return aClass != null;
                }
            });
        } else if (Env.isDevEnv()) {
            LOG.info("Authentication: No authentication in Dev-Environment");
        } else {
            LOG.warning("Unexpected auth method: " + Env.getAuthMethod());
            auth.authenticationProvider(new AuthenticationProvider() {
                @Override
                public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                    throw new BadCredentialsException("Invalid auth method configured");
                }

                @Override
                public boolean supports(Class<?> aClass) {
                    return aClass != null;
                }
            });
        }

    }
}
