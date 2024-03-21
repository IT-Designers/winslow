package de.itdesigners.winslow;

public class JavaThreadSleepWrapper {

    public void sleep(long timeInMilliseconds) throws InterruptedException {
        Thread.sleep(timeInMilliseconds);
    }
}
