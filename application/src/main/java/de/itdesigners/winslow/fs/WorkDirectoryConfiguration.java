package de.itdesigners.winslow.fs;

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
        return getRunDirectory().resolve("events");
    }

    @Nonnull
    default Path getPipelinesDirectory() {
        return getPath().resolve("pipelines");
    }

    @Nonnull
    default Path getLogsDirectory() {
        return getPath().resolve("logs");
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
        return getPath().resolve("run");
    }

    @Nonnull
    default Path getNodesDirectory() {
        return getRunDirectory().resolve("nodes");
    }

    @Nonnull
    default Path getSettingsDirectory() {
        return getPath().resolve("settings");
    }
}
