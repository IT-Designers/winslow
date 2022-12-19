package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.config.Image;

import javax.annotation.Nonnull;

public class ImageInfoConverter {

    @Nonnull
    public static ImageInfo from(@Nonnull Image image) {
        return new ImageInfo(
                image.getName(),
                image.getArgs(),
                image.getShmSizeMegabytes().orElse(0L)
        );
    }
}
