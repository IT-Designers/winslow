package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;

public record StageAssignedWorkspace(
        @Nonnull String absolutePath
) {
}
