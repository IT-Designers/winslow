package de.itdesigners.winslow.api.settings;

import javax.annotation.Nullable;

public class UserResourceLimitation {
    public final @Nullable Long cpu;
    public final @Nullable Long mem;
    public final @Nullable Long gpu;

    public UserResourceLimitation(@Nullable Long cpu, @Nullable Long mem, @Nullable Long gpu) {
        this.cpu = cpu;
        this.mem = mem;
        this.gpu = gpu;
    }
}
