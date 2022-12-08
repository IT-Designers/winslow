package de.itdesigners.winslow.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class PlatformInfo {

    public final @Nullable Integer maxFrequencyCpu;

    public PlatformInfo(@Nullable Integer maxFrequencyCpu) {
        this.maxFrequencyCpu = maxFrequencyCpu;
    }

    @Nonnull
    public Optional<Integer> getCpuSingleCoreMaxFrequencyMhz() {
        return Optional.ofNullable(maxFrequencyCpu);
    }
}
