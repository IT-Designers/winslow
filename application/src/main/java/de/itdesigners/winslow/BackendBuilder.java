package de.itdesigners.winslow;

import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

public interface BackendBuilder {

    @Nonnull
    Optional<PlatformInfo> tryRetrievePlatformInfoNoThrows();

    @Nonnull
    Backend create() throws IOException;
}
