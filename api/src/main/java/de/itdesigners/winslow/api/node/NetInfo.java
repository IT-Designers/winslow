package de.itdesigners.winslow.api.node;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NetInfo netInfo = (NetInfo) o;
        return transmitting == netInfo.transmitting && receiving == netInfo.receiving;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transmitting, receiving);
    }

    @Override
    public String toString() {
        return "NetInfo{" +
                "transmitting=" + transmitting +
                ", receiving=" + receiving +
                "}@" + hashCode();
    }
}
