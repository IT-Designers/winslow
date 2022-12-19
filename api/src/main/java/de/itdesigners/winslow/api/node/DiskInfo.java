package de.itdesigners.winslow.api.node;

/**
 * All units are bytes or bytes per second
 */
public record DiskInfo(
        long reading,
        long writing,
        long free,
        long used) {
}
