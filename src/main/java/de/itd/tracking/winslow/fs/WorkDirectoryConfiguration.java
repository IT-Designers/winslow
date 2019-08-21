package de.itd.tracking.winslow.fs;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface WorkDirectoryConfiguration {

    @Nonnull
    Path getPath();
}
