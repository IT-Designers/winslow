package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.List;

public class CpuInfo {

    private final @Nonnull String      modelName;
    private final @Nonnull List<Float> utilization;

    public CpuInfo(@Nonnull String modelName, @Nonnull List<Float> utilization) {
        this.modelName   = modelName;
        this.utilization = utilization;
    }

    @Nonnull
    public String getModelName() {
        return modelName;
    }

    @Nonnull
    public List<Float> getUtilization() {
        return utilization;
    }
}
