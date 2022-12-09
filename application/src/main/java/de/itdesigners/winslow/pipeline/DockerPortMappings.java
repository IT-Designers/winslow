package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record DockerPortMappings(
        @Nonnull List<Entry> mappings
) implements Extension {

    public DockerPortMappings(@Nonnull List<Entry> mappings) {
        this.mappings = Collections.unmodifiableList(mappings);
    }

    public DockerPortMappings(@Nonnull Entry...mappings) {
        this(Arrays.asList(mappings));
    }

    public record Entry(
            @Nullable String hostInterfaceIp,
            int hostPort,
            int containerPort
    ) {}
}
