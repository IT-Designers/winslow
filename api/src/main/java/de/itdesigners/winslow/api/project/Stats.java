package de.itdesigners.winslow.api.project;

public class Stats {

    public final float cpuUsed;
    public final float cpuMaximum;
    public final long  memoryAllocated;
    public final long  memoryMaximum;

    public Stats(float cpuUsed, float cpuMaximum, long memoryAllocated, long memoryMaximum) {
        this.cpuUsed         = cpuUsed;
        this.cpuMaximum      = cpuMaximum;
        this.memoryAllocated = memoryAllocated;
        this.memoryMaximum   = memoryMaximum;
    }
}
