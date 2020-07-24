package de.itdesigners.winslow.api.pipeline;

import java.beans.ConstructorProperties;
import java.beans.Transient;

public class RangeWithStepSize {

    private final float min;
    private final float max;
    private final float stepSize;

    @ConstructorProperties({"min", "max", "stepSize"})
    public RangeWithStepSize(float min, float max, float stepSize) {
        this.min      = Math.min(min, max);
        this.max      = Math.max(min, max);
        this.stepSize = Math.abs(stepSize);
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStepSize() {
        return stepSize;
    }

    @Transient
    public float getValue(int step) {
        var current = ((float)step) * this.stepSize;
        return Math.min(this.min + current, this.max);
    }

    @Transient
    public int getStepCount() {
        return Math.max(0, (int) Math.ceil(Math.abs((this.max - this.min) / this.stepSize))) + 1;
    }
}
