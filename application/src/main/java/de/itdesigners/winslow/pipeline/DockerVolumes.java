package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public class DockerVolumes implements Extension {

    private final @Nonnull List<DockerVolume> volumes;

    public DockerVolumes(@Nonnull List<DockerVolume> volumes) {
        this.volumes = volumes;
    }

    @Nonnull
    public List<DockerVolume> getVolumes() {
        return volumes;
    }
}
