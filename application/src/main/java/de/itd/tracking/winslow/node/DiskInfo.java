package de.itd.tracking.winslow.node;

/**
 * All units are bytes or bytes per second
 */
public class DiskInfo {

    private final long reading;
    private final long writing;


    public DiskInfo(long reading, long writing) {
        this.reading = reading;
        this.writing = writing;
    }

    public long getReading() {
        return reading;
    }

    public long getWriting() {
        return writing;
    }
}
