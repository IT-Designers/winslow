package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class CpuUtilization {

    private final @Nonnull List<Float> cpus;

    public CpuUtilization(@Nonnull List<Float> cpus) {
        this.cpus = cpus;
    }

    @Nonnull
    public Iterable<Float> getCpus() {
        return Collections.unmodifiableList(this.cpus);
    }
}
