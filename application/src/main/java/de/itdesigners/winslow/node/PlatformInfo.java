package de.itdesigners.winslow.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class PlatformInfo {

    @Deprecated
    public final @Nullable Integer maxFrequencyCpu;

    public PlatformInfo() {
        this.maxFrequencyCpu = null;
    }

    @Deprecated
    public PlatformInfo(@Nullable Integer maxFrequencyCpu) {
        this.maxFrequencyCpu = maxFrequencyCpu;
    }

    @Nonnull
    @Deprecated
    public Optional<Integer> getCpuSingleCoreMaxFrequency() {
        return Optional.ofNullable(maxFrequencyCpu);
    }
}
