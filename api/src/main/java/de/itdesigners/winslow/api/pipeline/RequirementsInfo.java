package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public record RequirementsInfo(
        int cpus,
        long megabytesOfRam,
        @Nonnull GpuRequirementsInfo gpu,
        @Nonnull List<String> tags) {

}
