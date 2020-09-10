package de.itdesigners.winslow.web.webdav;

import de.itdesigners.winslow.Winslow;
import io.milton.http.fs.NullSecurityManager;
import io.milton.servlet.DefaultMiltonConfigurator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
    static Optional<Winslow> getWinslow() {
        return Optional
                .ofNullable(context.get())
                .map(ApplicationContext::getAutowireCapableBeanFactory)
                .map(c -> c.getBean(Winslow.class));
    }


    @Override
    protected void build() {
        // security is implemented in the controller and spring auth
        builder.setSecurityManager(new NullSecurityManager());
        super.build();
    }
}
