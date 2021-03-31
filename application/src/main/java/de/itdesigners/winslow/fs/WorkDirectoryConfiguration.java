package de.itdesigners.winslow.fs;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;

public interface WorkDirectoryConfiguration {

    @Nonnull
    Path getPath();

    @Nonnull
    Optional<DockerVolumeTargetConfiguration> getDockerVolumeConfiguration(@Nonnull Path path);

    @Nonnull
    default Path getProjectsDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getPath().resolve("projects").toUri());
    }

    @Nonnull
    default Path getEventsDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getRunDirectory().resolve("events").toUri());
    }

    @Nonnull
    default Path getPipelinesDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getPath().resolve("pipelines").toUri());
    }

    @Nonnull
    default Path getLogsDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getPath().resolve("logs").toUri());
    }

    /**
     * See Unix '/run':
     * - (early) directory in which process and runtime info is stored,
     * which should not be deleted periodically, but does not contain
     * data that needs to persist (through a whole system reboot/restart)
     * <p>
     * The content of this directory changes a lot (many writes), therefore
     * it is not recommended to have it located on a SSD drive. Consider tmpfs.
     *
     * @return The absolute {@link Path} to it
     */
    @Nonnull
    default Path getRunDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getPath().resolve("run").toUri());
    }

    @Nonnull
    default Path getNodesDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getRunDirectory().resolve("nodes").toUri());
    }

    @Nonnull
    default Path getSettingsDirectory() {
        // prevent the FileSystem from leaking through
        return Path.of(getPath().resolve("settings").toUri());
    }
}
