package de.itd.tracking.winslow.node;

/**
 * All return units are bytes
 */
public interface MemInfo {

    long getMemoryTotal();

    long getMemoryFree();

    long getSystemCache();

    long getSwapTotal();

    long getSwapFree();
}
