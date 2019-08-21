package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface Poll<V, E extends Throwable> {

    @Nonnull
    Optional<V> poll() throws E;
}
