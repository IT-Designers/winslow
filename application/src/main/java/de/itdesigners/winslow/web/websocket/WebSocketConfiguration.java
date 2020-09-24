package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Env;
import org.springframework.context.ApplicationListener;
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
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    private static final @Nonnull Logger LOG = Logger.getLogger(PermissionCheckedInterceptor.class.getSimpleName());

    private static final Map<String, String> SESSION_ID_TO_USER = new ConcurrentHashMap<>();

    @Configuration
    public static class SessionDisconnectedListener implements ApplicationListener<SessionDisconnectEvent> {
        @Override
        public void onApplicationEvent(SessionDisconnectEvent event) {
            SESSION_ID_TO_USER.remove(event.getSessionId());
        }
    }

    @Configuration
    public static class SessionConnectedListener implements ApplicationListener<SessionConnectedEvent> {
        @Override
        public void onApplicationEvent(SessionConnectedEvent event) {
            var stomp = StompHeaderAccessor.wrap(event.getMessage());
            Optional
                    .ofNullable(event.getUser())
                    .map(Principal::getName)
                    .ifPresent(user -> SESSION_ID_TO_USER.put(stomp.getSessionId(), user));
        }
    }


    @Override
    public void configureMessageBroker(@Nonnull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(
                ProjectsEndpointController.TOPIC_PREFIX,
                NodesEndpointController.TOPIC_PREFIX
        );
        registry.setApplicationDestinationPrefixes("/");
    }

    @Override
    public void registerStompEndpoints(@Nonnull StompEndpointRegistry registry) {
        registry.addEndpoint(Env.getWebsocketPath()).setAllowedOrigins("*");
        registry.addEndpoint(Env.getWebsocketPath()).setAllowedOrigins("*").withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(10 * 1024 * 1024);
        registration.setSendBufferSizeLimit(10 * 1024 * 1024);
        registration.setSendTimeLimit(5_000);
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
        // return Env.isDevEnv();
        return true;
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
                            var user = Optional
                                    .ofNullable(header.getUser())
                                    .map(Principal::getName)
                                    .or(() -> Optional.ofNullable(SESSION_ID_TO_USER.get(header.getSessionId())));
                            var allowed = ((PrincipalPermissionChecker) checker).allowed(user.orElse(null));
                            LOG.fine("Message allowed to be delivered to user " + user + ": " + allowed);
                            LOG.fine("SessionId=" + header.getSessionId() + ", " + header.getSessionAttributes());
                            return allowed;
                        }
                        return true;
                    })
                    .orElse(null);
        }
    }

}
