package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;

public class ImageInfo {

    @Nullable public final String   name;
    @Nullable public final String[] args;
    @Nullable public final Integer  shmMegabytes;

//    @ConstructorProperties({"name, args, shmMegabytes"})
    public ImageInfo(@Nullable String name, @Nullable String[] args, @Nullable Integer shmMegabytes) {
        this.name         = name;
        this.args         = args;
        this.shmMegabytes = shmMegabytes;
    }
}
