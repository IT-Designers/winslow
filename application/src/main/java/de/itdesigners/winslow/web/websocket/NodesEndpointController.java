package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.node.NodeInfo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

@Controller
public class NodesEndpointController {

    public static final @Nonnull String TOPIC_PREFIX = "/nodes";
    public static final @Nonnull String TOPIC_NODES  = TOPIC_PREFIX;

    private final @Nonnull Winslow winslow;
    private final @Nonnull MessageSender sender;

    public NodesEndpointController(@Nonnull SimpMessagingTemplate simp, @Nonnull Winslow winslow) {
        this.winslow = winslow;
        this.sender  = new MessageSender(simp);

        this.winslow.getNodeRepository().addChangeListener((type, name) -> {
            var event = this.winslow
                    .getNodeRepository()
                    .getNodeInfo(name)
                    .filter(info -> ChangeEvent.ChangeType.DELETE != type)
                    .map(info -> new ChangeEvent<>(type, name, info))
                    .orElseGet(() -> new ChangeEvent<>(ChangeEvent.ChangeType.DELETE, name, null));
            sender.publishForAnybody(TOPIC_NODES, List.of(event));
        });
    }

    @SubscribeMapping(TOPIC_NODES)
    public Stream<ChangeEvent<String, NodeInfo>> subscribeNodes() {
        return winslow
                .getNodeRepository()
                .loadActiveNodes()
                .map(node -> new ChangeEvent<>(ChangeEvent.ChangeType.CREATE, node.getName(), node));
    }

}
