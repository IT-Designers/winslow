package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public class RangedList implements RangedValue {

    private final @Nonnull String[] values;

    public RangedList(@Nonnull String[] values) {
        this.values = values;
    }


    @Override
    public String getValue(int step) {
        if (values.length > 0) {
            return values[Math.max(0, Math.min(values.length - 1, step))];
        } else {
            return "";
        }
    }

    @Override
    public int getStepCount() {
        return values.length;
    }
}
