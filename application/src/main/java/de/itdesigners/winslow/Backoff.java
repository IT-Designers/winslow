package de.itdesigners.winslow;

public class Backoff {

    private final int   min;
    private final int   max;
    private final float multiplier;
    private       float sleepInMs = 0;

    private final JavaThreadSleepWrapper javaThreadSleepWrapper;

    public Backoff(int min, int max, float multiplier, final JavaThreadSleepWrapper javaThreadSleepWrapper) {
        this.min        = min;
        this.max        = max;
        this.multiplier = multiplier;
        this.javaThreadSleepWrapper = javaThreadSleepWrapper;
    }

    public void grow() {
        sleepInMs = Math.min(max, Math.max(min, sleepInMs * multiplier));
    }

    public void reset() {
        sleepInMs = min;
    }

    public long getSleepInMs() {
        return (long) sleepInMs;
    }

    public void sleep() {
        this.sleep(Long.MAX_VALUE);
    }

    public void sleep(long max) {
        try {
            javaThreadSleepWrapper.sleep(Math.min(max, getSleepInMs()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            grow();
        }
    }
}
