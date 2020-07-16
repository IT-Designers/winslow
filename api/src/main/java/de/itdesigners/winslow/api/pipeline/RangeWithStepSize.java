package de.itdesigners.winslow.api.pipeline;

import java.beans.ConstructorProperties;
import java.beans.Transient;

public class RangeWithStepSize {

    private final float min;
    private final float max;
    private final float stepSize;

    @ConstructorProperties({"min", "max", "stepSize"})
    public RangeWithStepSize(float min, float max, float stepSize) {
        this.min      = min;
        this.max      = max;
        this.stepSize = stepSize;
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
        var range    = this.max - this.min;
        var stepSize = this.stepSize;

        if (range < 0 != stepSize < 0) {
            stepSize = stepSize * -1.0f;
        }

        var current = this.min + (((float)step) * stepSize);

        return Math.min(current, this.max);
    }

    @Transient
    public int getStepCount() {
        return Math.max(0, (int) Math.ceil(Math.abs((this.max - this.min) / this.stepSize))) + 1;
    }
}
