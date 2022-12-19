package de.itdesigners.winslow.api.node;

/**
 * All return units are bytes
 */
public record MemInfo(
        long memoryTotal,
        long memoryFree,
        long systemCache,
        long swapTotal,
        long swapFree) {

}
