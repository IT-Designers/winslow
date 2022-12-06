package de.itdesigners.winslow.config;

import de.itdesigners.winslow.asblr.LogParserRegisterer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
            @Nullable String type) {
        this.matcher     = matcher;
        this.destination = destination;
        this.formatter   = formatter;
        this.type        = type != null ? type : LogParserRegisterer.PARSER_TYPE_REGEX_MATCHER_CSV;
    }


}
