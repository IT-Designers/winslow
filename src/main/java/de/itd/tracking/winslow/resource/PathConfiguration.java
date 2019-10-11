package de.itd.tracking.winslow.resource;

import java.nio.file.Path;

public class PathConfiguration {

    private final Path internal;
    private final Path resources;
    private final Path workspaces;
    private final Path pipelines;


    public PathConfiguration() {
        internal   = Path.of("winslow");
        resources  = Path.of("resources");
        workspaces = Path.of("workspaces");
        pipelines  = Path.of("pipelines");
    }

    public Path getRelativePathOfInternal() {
        return internal;
    }

    public Path resolvePathOfInternal(Path workDirectory) {
        return workDirectory.resolve(getRelativePathOfInternal());
    }

    public Path getRelativePathOfResources() {
        return resources;
    }

    public Path resolvePathOfResources(Path workDirectory) {
        return workDirectory.resolve(getRelativePathOfResources());
    }

    public Path getRelativePathOfWorkspaces() {
        return workspaces;
    }

    public Path resolvePathOfWorkspaces(Path workDirectory) {
        return workDirectory.resolve(getRelativePathOfWorkspaces());
    }

    public Path getRelativePathOfPipelines() {
        return pipelines;
    }

    public Path resolvePathOfPipelines(Path workDirectory) {
        return workDirectory.resolve(getRelativePathOfPipelines());
    }
}
