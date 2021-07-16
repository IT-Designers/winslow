package de.itdesigners.winslow.api.node;

import java.util.Objects;

public class GpuUtilization {

    public final float computeUtilization;
    public final float memoryUtilization;
    public final long  memoryUsedMegabytes;
    public final long  memoryTotalMegabytes;


    public GpuUtilization(
            float computeUtilization,
            float memoryUtilization,
            long memoryUsedMegabytes,
            long memoryTotalMegabytes
    ) {
        this.computeUtilization   = computeUtilization;
        this.memoryUtilization    = memoryUtilization;
        this.memoryUsedMegabytes  = memoryUsedMegabytes;
        this.memoryTotalMegabytes = memoryTotalMegabytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GpuUtilization that = (GpuUtilization) o;
        return Float.compare(
                that.computeUtilization,
                computeUtilization
        ) == 0 && Float.compare(
                that.memoryUtilization,
                memoryUtilization
        ) == 0 && memoryUsedMegabytes == that.memoryUsedMegabytes && memoryTotalMegabytes == that.memoryTotalMegabytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(computeUtilization, memoryUtilization, memoryUsedMegabytes, memoryTotalMegabytes);
    }

    @Override
    public String toString() {
        return "GpuUtilization{" +
                "computeUtilization=" + computeUtilization +
                ", memoryUtilization=" + memoryUtilization +
                ", memoryUsageMegabytes=" + memoryUsedMegabytes +
                ", memoryTotalMegabytes=" + memoryTotalMegabytes +
                "}@" + hashCode();
    }
}
