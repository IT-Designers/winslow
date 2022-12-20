package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record ParseError(
        int line,
        int column,
        @Nonnull String message) {

}
