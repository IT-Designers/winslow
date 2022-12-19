package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.config.Requirements;

import javax.annotation.Nonnull;

public class ResourceInfoConverter {

    @Nonnull
    public static ResourceInfo from(@Nonnull Requirements req) {
        return new ResourceInfo(
                req.getCpus().orElse(0),
                req.getMegabytesOfRam().orElse(0L),
                req.getGpu().getCount()
        );
    }
}
