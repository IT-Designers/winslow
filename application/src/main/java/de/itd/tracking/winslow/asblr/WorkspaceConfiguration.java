package de.itd.tracking.winslow.asblr;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class WorkspaceConfiguration {


    @Nonnull private final Path resourcesRelative;
    @Nonnull private final Path workspaceRelative;
    @Nonnull private final Path pipelineResourcesRelative;
    @Nonnull private final Path pipelineUnstagedRelative;

    @Nonnull private final Path resourcesAbsolute;
    @Nonnull private final Path workspaceAbsolute;
    @Nonnull private final Path pipelineResourcesAbsolute;
    @Nonnull private final Path pipelineUnstagedAbsolute;

    public WorkspaceConfiguration(
            @Nonnull Path resourcesRelative,
            @Nonnull Path workspaceRelative,
            @Nonnull Path pipelineResourcesRelative,
            @Nonnull Path pipelineUnstagedRelative,
            @Nonnull Path resourcesAbsolute,
            @Nonnull Path workspaceAbsolute,
            @Nonnull Path pipelineResourcesAbsolute,
            @Nonnull Path pipelineUnstagedAbsolute) {
        this.resourcesRelative         = resourcesRelative;
        this.workspaceRelative         = workspaceRelative;
        this.pipelineResourcesRelative = pipelineResourcesRelative;
        this.pipelineUnstagedRelative  = pipelineUnstagedRelative;
        this.resourcesAbsolute         = resourcesAbsolute;
        this.workspaceAbsolute         = workspaceAbsolute;
        this.pipelineResourcesAbsolute = pipelineResourcesAbsolute;
        this.pipelineUnstagedAbsolute  = pipelineUnstagedAbsolute;
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
    public Path getPipelineResourcesDirectory() {
        return pipelineResourcesRelative;
    }

    @Nonnull
    public Path getPipelineResourcesDirectoryAbsolute() {
        return pipelineResourcesAbsolute;
    }

    @Nonnull
    public Path getWorkspaceDirectory() {
        return workspaceRelative;
    }

    @Nonnull
    public Path getWorkspaceDirectoryAbsolute() {
        return workspaceAbsolute;
    }

    @Nonnull
    public Path getPipelineUnstagedDirectory() {
        return pipelineUnstagedRelative;
    }

    @Nonnull
    public Path getPipelineUnstagedDirectoryAbsolute() {
        return pipelineUnstagedAbsolute;
    }
}
