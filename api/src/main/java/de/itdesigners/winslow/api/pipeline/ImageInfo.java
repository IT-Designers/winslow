package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;

public class ImageInfo {

    @Nullable public final String   name;
    @Nullable public final String[] args;

    public ImageInfo(@Nullable String name, @Nullable String[] args) {
        this.name = name;
        this.args = args;
    }
}
