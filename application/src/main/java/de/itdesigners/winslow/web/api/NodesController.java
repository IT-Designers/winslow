package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.api.node.NodeResourceUsageConfiguration;
import de.itdesigners.winslow.api.node.NodeUtilization;
import de.itdesigners.winslow.auth.Group;
import de.itdesigners.winslow.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;
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

    /***
     * @param name The name of the node to retrieve the utilization for
     * @param from Timestamp-millis for the start time range
     * @param to Timestamp-millis for the end time range
     * @param chunkSpanMillis Milliseconds to chunk values into
     * @return Time series of {@link NodeUtilization}
     */
    @GetMapping("/nodes/{name}/utilization")
    public Stream<NodeUtilization> getNodeUtilization(
            @PathVariable("name") String name,
            @RequestParam(required = false, name = "from") @Nullable Long from,
            @RequestParam(required = false, name = "to") @Nullable Long to,
            @RequestParam(required = false, name = "chunkSpanMillis") @Nullable Long chunkSpanMillis
    ) {
        return winslow
                .getNodeRepository()
                .getNodeUtilizationBetween(
                        name,
                        from != null ? from : System.currentTimeMillis() - Duration.ofMinutes(1).toMillis(),
                        to != null ? to : System.currentTimeMillis(),
                        chunkSpanMillis != null ? chunkSpanMillis : 1
                );
    }


    /***
     * @param name The name of the node to retrieve the configuration for
     * @return The {@link NodeResourceUsageConfiguration} for the given node name
     */
    @Nonnull
    @GetMapping("/nodes/{name}/resource-usage-configuration")
    public Optional<NodeResourceUsageConfiguration> getNodeResourceUsageConfiguration(
            @PathVariable("name") String name
    ) {
        return winslow
                .getNodeRepository()
                .getNodeResourceLimitConfiguration(name);
    }

    @PutMapping("/nodes/{name}/resource-usage-configuration")
    public void setNodeResourceUsageConfiguration(
            @Nullable User user,
            @PathVariable("name") String name,
            @RequestBody NodeResourceUsageConfiguration configuration
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var canEdit = user.hasSuperPrivileges();

        if (!canEdit) {
            canEdit = winslow
                    .getNodeRepository()
                    .getNodeResourceLimitConfiguration(name)
                    .map(limit -> {
                        // all the groups the user ist part of
                        var userGroups = user.getGroups().stream().map(Group::name).toList();

                        return limit
                                .groupLimits()
                                .entrySet()
                                .stream()
                                // after this: all the entries (groups) that can edit the configuration
                                .filter(e -> e.getValue().role() == Role.OWNER)
                                .map(Map.Entry::getKey)
                                .anyMatch(userGroups::contains);
                    })
                    .orElse(Boolean.FALSE);
        }

        if (canEdit) {
            winslow
                    .getNodeRepository()
                    .setNodeResourceLimitConfiguration(
                            name,
                            configuration
                    );
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

    }
}
