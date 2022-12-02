package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.RequirementsInfo;
import de.itdesigners.winslow.config.Requirements;

import javax.annotation.Nonnull;

public class RequirementsInfoConverter {

    @Nonnull
    public static RequirementsInfo from(@Nonnull Requirements requirements) {
        return new RequirementsInfo(
                requirements.getCpus(),
                requirements.getMegabytesOfRam(),
                from(requirements.getGpu()),
                requirements.getTags()
        );
    }

    public static RequirementsInfo.GPUInfo from(@Nonnull Requirements.Gpu gpu) {
        return new RequirementsInfo.GPUInfo(gpu.getCount(), gpu.getVendor().orElse(""), gpu.getSupport());
    }

}
