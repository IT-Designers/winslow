package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;

public record LogParser(
        @Nonnull String matcher,
        @Nonnull String destination,
        @Nonnull String formatter,
        @Nonnull String type
) {

    public LogParser(
            @Nonnull String matcher,
            @Nonnull String destination,
            @Nonnull String formatter,
            @Nonnull String type) {
        this.matcher     = matcher;
        this.destination = destination;
        this.formatter   = formatter;
        this.type        = type;
    }


}
