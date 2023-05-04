package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.GpuRequirementsInfo;
import de.itdesigners.winslow.api.pipeline.RequirementsInfo;
import de.itdesigners.winslow.config.Requirements;

import javax.annotation.Nonnull;

public class RequirementsInfoConverter {

    @Nonnull
    public static RequirementsInfo from(@Nonnull Requirements requirements) {
        return new RequirementsInfo(
                requirements.getCpus().orElse(0),
                requirements.getMegabytesOfRam().orElse(0L),
                from(requirements.getGpu()),
                requirements.getTags()
        );
    }

    @Nonnull
    public static GpuRequirementsInfo from(@Nonnull Requirements.Gpu gpu) {
        return new GpuRequirementsInfo(gpu.getCount(), gpu.getVendor().orElse(""), gpu.getSupport());
    }

    @Nonnull
    public static Requirements reverse(@Nonnull RequirementsInfo requirements) {
        return new Requirements(
                requirements.cpus(),
                requirements.megabytesOfRam(),
                reverse(requirements.gpu()),
                requirements.tags()
        );
    }

    @Nonnull
    public static Requirements.Gpu reverse(@Nonnull GpuRequirementsInfo gpu) {
        return new Requirements.Gpu(
                gpu.count(),
                gpu.vendor(),
                gpu.support()
        );
    }
}
