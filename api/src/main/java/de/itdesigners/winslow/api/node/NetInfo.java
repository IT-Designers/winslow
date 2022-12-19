package de.itdesigners.winslow.api.node;

/**
 * All units are bytes or bytes per second
 */
public record NetInfo(
        long receiving,
        long transmitting) {
}
