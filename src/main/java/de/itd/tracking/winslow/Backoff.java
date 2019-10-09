package de.itd.tracking.winslow;

public class Backoff {

    private final int   min;
    private final int   max;
    private final float multiplier;
    private       float value;

    public Backoff(int min, int max, float multiplier) {
        this.min = min;
        this.max = max;
        this.multiplier = multiplier;
    }

    public void grow() {
        value = Math.min(max, Math.max(min, value * multiplier));
    }

    public void reset() {
        value = min;
    }

    public long getSleepMs() {
        return (long) value;
    }

    public void sleep() {
        try {
            Thread.sleep(getSleepMs());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            grow();
        }
    }
}
