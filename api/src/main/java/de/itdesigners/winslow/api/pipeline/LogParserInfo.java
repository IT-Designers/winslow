package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;

public record LogParserInfo(
        @Nonnull String matcher,
        @Nonnull String destination,
        @Nonnull String formatter,
        @Nonnull String type) {
}
