package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface BackendBuilder {

    @Nonnull
    Backend create() throws IOException;
}
