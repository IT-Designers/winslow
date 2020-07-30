package de.itdesigners.winslow.api.pipeline;

public interface RangedValue {

    String getValue(int step);

    int getStepCount();
}
