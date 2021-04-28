package de.itdesigners.winslow.api.node;

import java.util.Objects;

/**
 * All units are bytes or bytes per second
 */
public class DiskInfo {

    private final long reading;
    private final long writing;
    private final long free;
    private final long used;


    public DiskInfo(long reading, long writing, long free, long used) {
        this.reading = reading;
        this.writing = writing;
        this.free    = free;
        this.used    = used;
    }

    public long getReading() {
        return reading;
    }

    public long getWriting() {
        return writing;
    }

    public long getFree() {
        return free;
    }

    public long getUsed() {
        return used;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DiskInfo diskInfo = (DiskInfo) o;
        return reading == diskInfo.reading && writing == diskInfo.writing && free == diskInfo.free && used == diskInfo.used;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reading, writing, free, used);
    }

    @Override
    public String toString() {
        return "DiskInfo{" +
                "reading=" + reading +
                ", writing=" + writing +
                ", free=" + free +
                ", used=" + used +
                "}@" + hashCode();
    }
}
