package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public class DockerNfsVolumes implements Extension {

    private final @Nonnull List<DockerNfsVolume> volumes;

    public DockerNfsVolumes(@Nonnull List<DockerNfsVolume> volumes) {
        this.volumes = volumes;
    }

    @Nonnull
    public List<DockerNfsVolume> getVolumes() {
        return volumes;
    }
}
