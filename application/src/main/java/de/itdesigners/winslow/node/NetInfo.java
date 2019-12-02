package de.itdesigners.winslow.node;

/**
 * All units are bytes or bytes per second
 */
public class NetInfo {

    private final long transmitting;
    private final long receiving;

    public NetInfo(long receiving, long transmitting) {
        this.receiving    = receiving;
        this.transmitting = transmitting;
    }

    public long getTransmitting() {
        return transmitting;
    }

    public long getReceiving() {
        return receiving;
    }
}
