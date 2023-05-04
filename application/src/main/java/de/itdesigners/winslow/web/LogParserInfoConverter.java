package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.LogParserInfo;
import de.itdesigners.winslow.config.LogParser;

import javax.annotation.Nonnull;

public class LogParserInfoConverter {

    @Nonnull
    public static LogParserInfo from(@Nonnull LogParser logParser) {
        return new LogParserInfo(
                logParser.matcher(),
                logParser.destination(),
                logParser.formatter(),
                logParser.type()
        );
    }

    @Nonnull
    public static LogParser reverse(@Nonnull LogParserInfo logParser) {
        return new LogParser(
                logParser.matcher(),
                logParser.destination(),
                logParser.formatter(),
                logParser.type()
        );
    }
}
