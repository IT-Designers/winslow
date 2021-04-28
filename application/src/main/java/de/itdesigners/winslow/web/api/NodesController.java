package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.api.node.NodeUtilization;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class NodesController {

    private final Winslow winslow;

    public NodesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/nodes")
    public Stream<NodeInfo> getAllNodeInfo() {
        try {
            return winslow
                    .getNodeRepository()
                    .listActiveNodes()
                    .map(winslow.getNodeRepository()::getNodeInfo)
                    .flatMap(Optional::stream);
        } catch (Throwable t) {
            t.printStackTrace();
            return Stream.empty();
        }
    }

    @GetMapping("/nodes/{name}")
    public Optional<NodeInfo> getNodeInfo(@PathVariable("name") String name) {
        return winslow.getNodeRepository().getNodeInfo(name);
    }

    @GetMapping("/nodes/{name}/utilization")
    public Stream<NodeUtilization> getNodeUtilization(
            @PathVariable("name") String name,
            @RequestParam(required = false, name = "from") @Nullable Long from,
            @RequestParam(required = false, name = "to") @Nullable Long to) {
        return winslow
                .getNodeRepository()
                .getNodeUtilizationBetween(
                        name,
                        from != null ? from : System.currentTimeMillis() - Duration.ofMinutes(1).toMillis(),
                        to != null ? to : System.currentTimeMillis()
                );
    }


}
