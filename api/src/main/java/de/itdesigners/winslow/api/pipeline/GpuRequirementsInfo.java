package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record GpuRequirementsInfo(
        int count,
        @Nonnull String vendor,
        @Nonnull String[] support) {
}
