package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;

public record DockerContainerNetworkLinkage(
        @Nonnull String containerTarget
) implements Extension {
}
