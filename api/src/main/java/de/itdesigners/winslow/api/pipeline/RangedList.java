package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.beans.Transient;

public class RangedList implements RangedValue {

    private final @Nonnull String[] values;

    @ConstructorProperties({"values"})
    public RangedList(@Nonnull String[] values) {
        this.values = values;
    }

    @Nonnull
    public String[] getValues() {
        return values;
    }

    @Override
    @Transient
    public String getValue(int step) {
        if (values.length > 0) {
            return values[Math.max(0, Math.min(values.length - 1, step))];
        } else {
            return "";
        }
    }

    @Override
    @Transient
    public int getStepCount() {
        return values.length;
    }
}
