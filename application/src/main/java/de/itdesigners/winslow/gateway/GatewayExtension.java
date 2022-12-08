package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Extension;

import javax.annotation.Nonnull;

public record GatewayExtension(
        @Nonnull StageDefinition stageDefinition
) implements Extension {
}
