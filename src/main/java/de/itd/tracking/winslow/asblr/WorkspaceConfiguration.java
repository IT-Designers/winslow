package de.itd.tracking.winslow.asblr;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class WorkspaceConfiguration {
    @Nonnull private final Path resources;
    @Nonnull private final Path workspace;

    public WorkspaceConfiguration(@Nonnull Path resources, @Nonnull Path workspace) {
        this.resources = resources;
        this.workspace = workspace;
    }

    @Nonnull
    public Path getResourcesDirectory() {
        return resources;
    }

    @Nonnull
    public Path getWorkspaceDirectory() {
        return workspace;
    }
}
