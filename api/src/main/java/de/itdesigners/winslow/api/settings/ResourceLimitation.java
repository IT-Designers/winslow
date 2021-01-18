package de.itdesigners.winslow.api.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class ResourceLimitation {

    public final @Nullable Long cpu;
    public final @Nullable Long mem;
    public final @Nullable Long gpu;

    public ResourceLimitation() {
        this(null, null, null);
    }

    public ResourceLimitation(@Nullable Long cpu, @Nullable Long mem, @Nullable Long gpu) {
        this.cpu = cpu;
        this.mem = mem;
        this.gpu = gpu;
    }

    @Nonnull
    public ResourceLimitation min(@Nonnull ResourceLimitation other) {
        return new ResourceLimitation(
                cpu == null ? other.cpu : Math.min(cpu, other.cpu != null ? other.cpu : Long.MAX_VALUE),
                mem == null ? other.mem : Math.min(mem, other.mem != null ? other.mem : Long.MAX_VALUE),
                gpu == null ? other.gpu : Math.min(gpu, other.gpu != null ? other.gpu : Long.MAX_VALUE)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ResourceLimitation that = (ResourceLimitation) o;
        return Objects.equals(cpu, that.cpu) &&
                Objects.equals(mem, that.mem) &&
                Objects.equals(gpu, that.gpu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpu, mem, gpu);
    }

    @Override
    public String toString() {
        return "ResourceLimitation{" +
                "cpu=" + cpu +
                ", mem=" + mem +
                ", gpu=" + gpu +
                "}@" + hashCode();
    }
}
