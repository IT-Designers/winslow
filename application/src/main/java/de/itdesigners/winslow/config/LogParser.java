package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;

public class LogParser {

    private final @Nonnull String matcher;
    private final @Nonnull String destination;
    private final @Nonnull String formatter;
    private final @Nonnull String type;

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

    @Nonnull
    public String getMatcher() {
        return matcher;
    }

    @Nonnull
    public String getDestination() {
        return destination;
    }

    @Nonnull
    public String getFormatter() {
        return formatter;
    }

    @Nonnull
    public String getType() {
        return type;
    }
}
