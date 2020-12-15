package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;

public class AllocInfo {

    private final @Nonnull String title;
    private final          long    cpu;
    private final          long    memory;
    private final          long    gpu;

    public AllocInfo(@Nonnull String title, long cpu, long memory, long gpu) {
        this.title  = title;
        this.cpu    = cpu;
        this.memory = memory;
        this.gpu    = gpu;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    public long getCpu() {
        return cpu;
    }

    public long getMemory() {
        return memory;
    }

    public long getGpu() {
        return gpu;


    }
}
