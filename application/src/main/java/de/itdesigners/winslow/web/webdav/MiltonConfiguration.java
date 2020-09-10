package de.itdesigners.winslow.web.webdav;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.fs.NullSecurityManager;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.Resource;
import io.milton.servlet.DefaultMiltonConfigurator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Optional;

@Component
public class MiltonConfiguration extends DefaultMiltonConfigurator implements ApplicationContextAware {

    private static WeakReference<ApplicationContext> context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MiltonConfiguration.context = new WeakReference<>(applicationContext);
    }

    @Nonnull
    private static Optional<Winslow> getWinslow() {
        return Optional
                .ofNullable(context.get())
                .map(ApplicationContext::getAutowireCapableBeanFactory)
                .map(c -> c.getBean(Winslow.class));
    }


    @Override
    protected void build() {
        NullSecurityManager sec = new SpringSecurityManager();
        sec.setRealm("Winslow");
        builder.setSecurityManager(sec);
        var context = SecurityContextHolder.getContext();
        var con     = context.getAuthentication();

        super.build();
    }

    private static class SpringSecurityManager extends NullSecurityManager {

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return super.authenticate(digestRequest);
        }

        @Override
        public Object authenticate(String user, String password) {
            var context = SecurityContextHolder.getContext();
            var auth    = context.getAuthentication();
            return getWinslow()
                    .map(Winslow::getUserRepository)
                    .flatMap(u -> u.getUserOrCreateAuthenticated(auth.getName()))
                    .orElse(null);
        }

        @Override
        public boolean authorise(
                Request request,
                Request.Method method,
                Auth auth,
                Resource resource) {
            return auth != null && auth.getTag() instanceof User;
        }

        @Override
        public String getRealm(String host) {
            return super.getRealm(host);
        }

        @Override
        public boolean isDigestAllowed() {
            return super.isDigestAllowed();
        }
    }
}
