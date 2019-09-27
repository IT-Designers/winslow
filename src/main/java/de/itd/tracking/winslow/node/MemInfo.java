package de.itd.tracking.winslow.node;

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
}
