package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Env;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@Nonnull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/projects", "/private", "/private/projects");
        registry.setApplicationDestinationPrefixes("/");
    }

    @Override
    public void registerStompEndpoints(@Nonnull StompEndpointRegistry registry) {
        registry.addEndpoint(Env.getWebsocketPath()).setAllowedOrigins("*");
        registry.addEndpoint(Env.getWebsocketPath()).setAllowedOrigins("*").withSockJS();
    }

    @Override
    protected void customizeClientInboundChannel(ChannelRegistration registration) {
        // registration.interceptors(new UserPermInterceptor());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new PermissionCheckedInterceptor());
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .anyMessage()
                .permitAll();
    }


    @Override
    protected boolean sameOriginDisabled() {
        return Env.isDevEnv();
        // return true;
    }

    private static class PermissionCheckedInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(@Nonnull Message<?> message, @Nullable MessageChannel channel) {
            return Optional
                    .of(message)
                    .filter(m -> {
                        var checker = m.getHeaders().get(MessageSender.PERMISSION_CHECKER_HEADER);
                        if (checker instanceof PrincipalPermissionChecker) {
                            var header = StompHeaderAccessor.wrap(m);
                            return ((PrincipalPermissionChecker) checker).allowed(header.getUser());
                        }
                        return true;
                    })
                    .orElse(null);
        }
    }

}
