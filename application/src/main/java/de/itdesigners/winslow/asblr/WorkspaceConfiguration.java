package de.itdesigners.winslow.asblr;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class WorkspaceConfiguration {


    @Nonnull private final Path resourcesRelative;
    @Nonnull private final Path workspaceRelative;
    @Nonnull private final Path pipelineInputRelative;
    @Nonnull private final Path pipelineOutputRelative;

    @Nonnull private final Path resourcesAbsolute;
    @Nonnull private final Path workspaceAbsolute;
    @Nonnull private final Path pipelineInputAbsolute;
    @Nonnull private final Path pipelineOutputAbsolute;

    public WorkspaceConfiguration(
            @Nonnull Path resourcesRelative,
            @Nonnull Path workspaceRelative,
            @Nonnull Path pipelineInputRelative,
            @Nonnull Path pipelineOutputRelative,
            @Nonnull Path resourcesAbsolute,
            @Nonnull Path workspaceAbsolute,
            @Nonnull Path pipelineInputAbsolute,
            @Nonnull Path pipelineOutputAbsolute) {
        this.resourcesRelative      = resourcesRelative;
        this.workspaceRelative      = workspaceRelative;
        this.pipelineInputRelative  = pipelineInputRelative;
        this.pipelineOutputRelative = pipelineOutputRelative;
        this.resourcesAbsolute      = resourcesAbsolute;
        this.workspaceAbsolute      = workspaceAbsolute;
        this.pipelineInputAbsolute  = pipelineInputAbsolute;
        this.pipelineOutputAbsolute = pipelineOutputAbsolute;
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
    public Path getPipelineInputDirectory() {
        return pipelineInputRelative;
    }

    @Nonnull
    public Path getPipelineInputDirectoryAbsolute() {
        return pipelineInputAbsolute;
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
    public Path getPipelineOutputDirectory() {
        return pipelineOutputRelative;
    }

    @Nonnull
    public Path getPipelineOutputDirectoryAbsolute() {
        return pipelineOutputAbsolute;
    }
}
