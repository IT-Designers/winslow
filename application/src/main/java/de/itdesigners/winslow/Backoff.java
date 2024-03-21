package de.itdesigners.winslow;

public class Backoff {

    private final int   min;
    private final int   max;
    private final float multiplier;
    private       float sleepMs = 0;

    private final JavaThreadSleepWrapper javaThreadSleepWrapper;

    public Backoff(int min, int max, float multiplier, final JavaThreadSleepWrapper javaThreadSleepWrapper) {
        this.min        = min;
        this.max        = max;
        this.multiplier = multiplier;
        this.javaThreadSleepWrapper = javaThreadSleepWrapper;
    }

    public void grow() {
        sleepMs = Math.min(max, Math.max(min, sleepMs * multiplier));
    }

    public void reset() {
        sleepMs = min;
    }

    public long getSleepMs() {
        return (long) sleepMs;
    }

    public void sleep() {
        this.sleep(Long.MAX_VALUE);
    }

    public void sleep(long max) {
        try {
            javaThreadSleepWrapper.sleep(Math.min(max, getSleepMs()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            grow();
        }
    }
}
