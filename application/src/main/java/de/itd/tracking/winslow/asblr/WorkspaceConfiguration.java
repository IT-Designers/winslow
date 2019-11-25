package de.itd.tracking.winslow.asblr;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class WorkspaceConfiguration {


    @Nonnull private final Path resourcesRelative;
    @Nonnull private final Path workspaceRelative;
    @Nonnull private final Path resourcesAbsolute;
    @Nonnull private final Path workspaceAbsolute;

    public WorkspaceConfiguration(
            @Nonnull Path resourcesRelative,
            @Nonnull Path workspaceRelative,
            @Nonnull Path resourcesAbsolute,
            @Nonnull Path workspace) {
        this.resourcesRelative = resourcesRelative;
        this.workspaceRelative = workspaceRelative;
        this.resourcesAbsolute = resourcesAbsolute;
        this.workspaceAbsolute = workspace;
    }

    @Nonnull
    public Path getResourcesDirectory() {
        return resourcesRelative;
    }

    @Nonnull
    public Path getResourcesDirectoryAbsolute() {
        return resourcesAbsolute;
    }

    @Nonnull
    public Path getWorkspaceDirectory() {
        return workspaceRelative;
    }

    @Nonnull
    public Path getWorkspaceDirectoryAbsolute() {
        return workspaceAbsolute;
    }
}
