package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.config.Requirements;

import javax.annotation.Nonnull;

public class ResourceInfoConverter {

    @Nonnull
    public static ResourceInfo from(@Nonnull Requirements req) {
        return new ResourceInfo(
                req.getCpu(),
                req.getMegabytesOfRam(),
                req.getGpu().getCount()
        );
    }
}
