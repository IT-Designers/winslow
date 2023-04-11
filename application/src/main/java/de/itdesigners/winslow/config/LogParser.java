package de.itdesigners.winslow.config;

import de.itdesigners.winslow.asblr.LogParserRegisterer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LogParser {

    private final @Nonnull String matcher;
    private final @Nonnull String destination;
    private final @Nonnull String formatter;
    private final @Nonnull String type;

    public LogParser(
            @Nonnull String matcher,
            @Nonnull String destination,
            @Nonnull String formatter,
            @Nullable String type) {
        this.matcher     = matcher;
        this.destination = destination;
        this.formatter   = formatter;
        this.type        = type != null ? type : LogParserRegisterer.PARSER_TYPE_REGEX_MATCHER_CSV;
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
