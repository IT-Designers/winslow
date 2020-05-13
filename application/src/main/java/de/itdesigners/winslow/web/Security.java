package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Env;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class Security extends WebSecurityConfigurerAdapter {


    private @Value("${winslow.ldap.user.search-base:#{null}}")             String userSearchBase;
    private @Value("${winslow.ldap.user.search-filter:#{\"(uid={0})\"}}")  String userSearchFilter;
    private @Value("${winslow.ldap.group.search-base:#{null}}")            String groupSearchBase;
    private @Value("${winslow.ldap.group.search-filter:#{\"(gid={0})\"}}") String groupSearchFilter;
    private @Value("${winslow.ldap.url:#{null}}")                          String ldapUrl;
    private @Value("${winslow.ldap.manager.dn:#{null}}")                   String managerDn;
    private @Value("${winslow.ldap.manager.password:#{null}}")             String managerPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureCsrfToken(http);

        if (!Env.isDevEnv()) {
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
                    .anyRequest().authenticated()
                    .and()
                    .formLogin().and()
                    .httpBasic();
        }
    }

    private void configureCsrfToken(HttpSecurity http) throws Exception {
        var repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");
        http.csrf().csrfTokenRepository(repo);
    }


    @Autowired
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (Env.isLdapAuthEnabled()) {
            auth.ldapAuthentication()
                .userSearchBase(userSearchBase)
                .userSearchFilter(userSearchFilter)
                .groupSearchBase(groupSearchBase)
                .groupSearchFilter(groupSearchFilter)
                .contextSource()
                .url(ldapUrl)
                .managerDn(managerDn)
                .managerPassword(managerPassword);
        } else {
            super.configure(auth);
        }
    }
}

