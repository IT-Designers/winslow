package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.node.NodeInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
        return winslow
                .getNodeRepository()
                .listActiveNodes()
                .map(winslow.getNodeRepository()::getNodeInfo)
                .flatMap(Optional::stream);
    }

    @GetMapping("/nodes/{name}")
    public Optional<NodeInfo> getNodeInfo(@PathVariable("name") String name) {
        return winslow.getNodeRepository().getNodeInfo(name);
    }
}
