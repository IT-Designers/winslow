package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;

public class ResourceInfo {
    public final           int     cpus;
    public final           int    megabytesOfRam;
    public final @Nullable Integer gpus;


    @ConstructorProperties({"cpus", "megabytesOfRam", "gpus"})
    public ResourceInfo(int cpus, int megabytesOfRam, @Nullable Integer gpus) {
        this.cpus           = cpus;
        this.megabytesOfRam = megabytesOfRam;
        this.gpus           = gpus;
    }
}
