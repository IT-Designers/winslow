package de.itdesigners.winslow.api.pipeline;

import java.beans.ConstructorProperties;
import java.beans.Transient;

public class RangeWithStepSize implements RangedValue {

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
    private boolean isIntegerRange() {
        return this.min == (float) (int) this.min
                && this.max == (float) (int) this.max
                && this.stepSize == (float) (int) this.stepSize;
    }

    @Override
    @Transient
    public String getValue(int step) {
        var current = ((float) Math.max(0, step)) * this.stepSize;
        var result  = Math.min(this.min + current, this.max);

        if (isIntegerRange()) {
            return String.valueOf((int)result);
        } else {
            return String.valueOf(result);
        }
    }

    @Override
    @Transient
    public int getStepCount() {
        return Math.max(0, (int) Math.ceil(Math.abs((this.max - this.min) / this.stepSize))) + 1;
    }
}
