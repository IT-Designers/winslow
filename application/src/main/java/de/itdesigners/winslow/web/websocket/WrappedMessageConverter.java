package de.itdesigners.winslow.web.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

class WrappedMessageConverter implements MessageConverter {
    private final @Nonnull MessageConverter parent;

    WrappedMessageConverter(@Nonnull MessageConverter parent) {
        this.parent = parent;
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        return this.parent.fromMessage(message, targetClass);
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        if (payload instanceof PermissionCheckedPayload) {
            var message = parent.toMessage(((PermissionCheckedPayload) payload).getValue(), headers);
            if (message != null) {
                var map = message.getHeaders().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
                map.put(MessageSender.PERMISSION_CHECKER_HEADER, ((PermissionCheckedPayload) payload).getChecker());
                return new GenericMessage<>(message.getPayload(), map);
            } else {
                return message;
            }
        } else {
            return parent.toMessage(payload, headers);
        }
    }
}
