package de.itdesigners.winslow.api.settings;

import javax.annotation.Nullable;

public class ResourceLimitation {

    public final @Nullable Long cpu;
    public final @Nullable Long mem;
    public final @Nullable Long gpu;

    public ResourceLimitation(@Nullable Long cpu, @Nullable Long mem, @Nullable Long gpu) {
        this.cpu = cpu;
        this.mem = mem;
        this.gpu = gpu;
    }
}
