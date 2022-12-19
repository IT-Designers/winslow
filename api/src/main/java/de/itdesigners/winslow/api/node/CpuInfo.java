package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.List;

public record CpuInfo(
        @Nonnull String modelName,
        @Nonnull List<Float> utilization) {

}
