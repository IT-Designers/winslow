package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import java.util.Optional;

public record UpdatePauseRequest(
        boolean paused,
        @Nonnull Optional<String> strategy) {
}
