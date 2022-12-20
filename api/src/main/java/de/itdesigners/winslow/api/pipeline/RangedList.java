package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.beans.Transient;

public record RangedList(@Nonnull String[] values) implements RangedValue {

    @ConstructorProperties({"values"})
    public RangedList {
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
    public int getStepCount() {
        return values.length;
    }
}
