package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.List;

public record CpuUtilization(
        @Nonnull List<Float> cpus) {

}
