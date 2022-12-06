package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public record RequirementsInfo(
        int cpus,
        @Nonnull Integer ram,
        @Nonnull GPUInfo gpu,
        @Nonnull List<String> tags


) {

    public record GPUInfo(
            int count,
            @Nonnull String vendor,
            @Nonnull String[] support) {

    }
}
