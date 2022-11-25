package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

import static de.itdesigners.winslow.web.webdav.WebDavController.EXPORT_NAME;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = Logger.getLogger(SecurityConfiguration.class.getSimpleName());

    private @Value("${winslow.ldap.user.search-base:#{null}}")             String userSearchBase;
    private @Value("${winslow.ldap.user.search-filter:#{\"(uid={0})\"}}")  String userSearchFilter;
    private @Value("${winslow.ldap.group.search-base:#{null}}")            String groupSearchBase;
    private @Value("${winslow.ldap.group.search-filter:#{\"(gid={0})\"}}") String groupSearchFilter;
    private @Value("${winslow.ldap.url:#{null}}")                          String ldapUrl;
    private @Value("${winslow.ldap.manager.dn:#{null}}")                   String managerDn;
    private @Value("${winslow.ldap.manager.password:#{null}}")             String managerPassword;

    private @Autowired Winslow winslow;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureCsrfToken(http);
        configureAuthorization(http);
        configureTransport(http);
    }

    private void configureTransport(HttpSecurity http) throws Exception {
        if (Env.requireSecure()) {
            http.requiresChannel().anyRequest().requiresSecure();
        }
    }

    private void configureAuthorization(HttpSecurity http) throws Exception {
        if (!Env.isDevEnv() || Env.isAuthMethodSet()) {
            LOG.info("Authorization: Requiring login to access Winslow");
            /*
            http
                    .authorizeRequests()
                    .antMatchers("/**")
                    .fullyAuthenticated()
                    .anyRequest()
                    .fullyAuthenticated()
                    .and()
                    .httpBasic()
                    .authenticationEntryPoint(new BasicAuth());*/
            http
                    .authorizeRequests()
                    .requestMatchers(new AntPathRequestMatcher(Env.getApiNoAuthPath() + "**")).permitAll()
                    .and()
                    .authorizeRequests()
                    .anyRequest()
                    .authenticated()
                    .and()
                    .formLogin().and()
                    .httpBasic();
        } else {
            LOG.info("Authorization: No login required to access Winslow");
        }
    }

    private void configureCsrfToken(HttpSecurity http) throws Exception {
        var repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");
        http.csrf().csrfTokenRepository(repo)
            .and().csrf().ignoringAntMatchers("/" + EXPORT_NAME + "/**")
            .and().csrf().ignoringAntMatchers(Env.getWebsocketPath() + "**")
            .and().headers().frameOptions().sameOrigin();

        http.addFilterAfter((servletRequest, servletResponse, filterChain) -> {
            if (servletRequest instanceof HttpServletRequest sr) {
                var user = Optional
                        .of(sr)
                        .filter(r -> r.getUserPrincipal() != null && r.getUserPrincipal().getName() != null)
                        .flatMap(r -> winslow.getUserManager().getUser(r.getUserPrincipal().getName()));

                if (user.isPresent() && !user.get().active()) {
                    sr.getSession().invalidate();
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);
        }, AuthorizationFilter.class);
    }


    @Autowired
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (Env.isLdapAuthEnabled()) {
            LOG.info("Authentication: Using LDAP authentication");
            auth.ldapAuthentication()
                .userSearchBase(userSearchBase)
                .userSearchFilter(userSearchFilter)
                .groupSearchBase(groupSearchBase)
                .groupSearchFilter(groupSearchFilter)
                .contextSource()
                .url(ldapUrl)
                .managerDn(managerDn)
                .managerPassword(managerPassword);
        } else if (Env.isLocalAuthEnabled()) {
            LOG.info("Authentication: Using built-in authentication");
            auth.authenticationProvider(new AuthenticationProvider() {
                @Override
                public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                    return Optional
                            .ofNullable(authentication.getCredentials())
                            .filter(cred -> cred instanceof String)
                            .map(Object::toString)
                            .flatMap(pw -> winslow
                                    .getUserManager()
                                    .getUser(authentication.getName().trim())
                                    .filter(user -> user.active()
                                            && user.password() != null
                                            && user.password().isPasswordCorrect(pw)
                                    )
                                    .map(user -> new UsernamePasswordAuthenticationToken(
                                            user.name(),
                                            null, // no need to store the password ... right?
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
            super.configure(auth);
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

