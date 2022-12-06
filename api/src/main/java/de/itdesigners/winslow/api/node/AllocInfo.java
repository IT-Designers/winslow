package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.Objects;

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

    @Nonnull
    public AllocInfo add(@Nonnull AllocInfo info) {
        return this.add(info, this.title);
    }

    @Nonnull
    public AllocInfo add(@Nonnull AllocInfo info, @Nonnull String title) {
        return new AllocInfo(
                title,
                this.cpu + info.cpu,
                this.memory + info.memory,
                this.gpu + info.gpu
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AllocInfo allocInfo = (AllocInfo) o;
        return cpu == allocInfo.cpu &&
                memory == allocInfo.memory &&
                gpu == allocInfo.gpu &&
                title.equals(allocInfo.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, cpu, memory, gpu);
    }

    @Override
    public String toString() {
        return "AllocInfo{" +
                "title='" + title + '\'' +
                ", cpus=" + cpu +
                ", memory=" + memory +
                ", gpu=" + gpu +
                "}@"+hashCode();
    }
}
