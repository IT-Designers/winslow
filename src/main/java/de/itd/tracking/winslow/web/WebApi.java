package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

@SpringBootApplication
public class WebApi {

    public static Context start(Winslow winslow) {
        var builder = new SpringApplicationBuilder(WebApi.class)
                .web(WebApplicationType.SERVLET);

        builder.application().addInitializers( (GenericApplicationContext context) -> {
            context.registerBean(Winslow.class, () -> winslow);
        });

        return new Context(builder.run());
    }

    public static class Context {
        private final ConfigurableApplicationContext context;

        private Context(ConfigurableApplicationContext context) {
            this.context = context;
        }

        public void stop() {
            this.context.close();
        }
    }
}
