package de.itd.tracking.winslow.fs;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface WorkDirectoryConfiguration {

    @Nonnull
    Path getPath();

    @Nonnull
    default Path getProjectsDirectory() {
        return getPath().resolve("projects");
    }

    @Nonnull
    default Path getEventsDirectory() {
        return getPath().resolve("events");
    }
}
