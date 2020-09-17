package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.project.Project;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

import static de.itdesigners.winslow.web.websocket.ProjectsEndpointController.encapsulate;
import static de.itdesigners.winslow.web.websocket.ProjectsEndpointController.getPermissionChecker;

public class MessageSender {

    public static final String PERMISSION_CHECKER_HEADER = "permission-checker";

    private final @Nonnull SimpMessagingTemplate simp;

    public MessageSender(@Nonnull SimpMessagingTemplate simp) {
        this.simp = simp;
        this.simp.setMessageConverter(getMessageConverter(simp.getMessageConverter()));
    }

    @Nonnull
    private MessageConverter getMessageConverter(@Nonnull MessageConverter messageConverter) {
        if (messageConverter instanceof WrappedMessageConverter) {
            return messageConverter;
        } else {
            return new WrappedMessageConverter(messageConverter);
        }
    }

    public void convertAndSend(
            @Nonnull String destination,
            @Nonnull Object value,
            @Nonnull PrincipalPermissionChecker permissionChecker) {
        this.simp.convertAndSend(destination, new PermissionCheckedPayload(permissionChecker, value));
    }




    public void publishProjectUpdate(
            @Nonnull Winslow winslow,
            @Nonnull String destination,
            @Nonnull String projectId,
            @Nullable Object value,
            @Nullable Project project) {
        this.convertAndSend(
                destination,
                value instanceof Collection<?>
                ? ((Collection<?>) value).stream().map(v -> encapsulate(projectId, v))
                : encapsulate(projectId, value),
                getPermissionChecker(winslow, project)
        );
    }

}
