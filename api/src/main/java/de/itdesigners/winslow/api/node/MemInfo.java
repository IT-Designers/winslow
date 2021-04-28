package de.itdesigners.winslow.api.node;

import java.util.Objects;

/**
 * All return units are bytes
 */
public class MemInfo {

    private final long memoryTotal;
    private final long memoryFree;
    private final long systemCache;
    private final long swapTotal;
    private final long swapFree;

    public MemInfo(long memoryTotal, long memoryFree, long systemCache, long swapTotal, long swapFree) {
        this.memoryTotal = memoryTotal;
        this.memoryFree  = memoryFree;
        this.systemCache = systemCache;
        this.swapTotal   = swapTotal;
        this.swapFree    = swapFree;
    }

    public long getMemoryTotal() {
        return memoryTotal;
    }

    public long getMemoryFree() {
        return memoryFree;
    }

    public long getSystemCache() {
        return systemCache;
    }

    public long getSwapTotal() {
        return swapTotal;
    }

    public long getSwapFree() {
        return swapFree;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MemInfo memInfo = (MemInfo) o;
        return memoryTotal == memInfo.memoryTotal && memoryFree == memInfo.memoryFree && systemCache == memInfo.systemCache && swapTotal == memInfo.swapTotal && swapFree == memInfo.swapFree;
    }

    @Override
    public int hashCode() {
        return Objects.hash(memoryTotal, memoryFree, systemCache, swapTotal, swapFree);
    }

    @Override
    public String toString() {
        return "MemInfo{" +
                "memoryTotal=" + memoryTotal +
                ", memoryFree=" + memoryFree +
                ", systemCache=" + systemCache +
                ", swapTotal=" + swapTotal +
                ", swapFree=" + swapFree +
                "}@" + hashCode();
    }
}
