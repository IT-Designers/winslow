package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LogParser {

    private final @Nonnull String      matcher;
    private final @Nonnull String      destination;
    private final @Nonnull String      formatter;
    private final @Nonnull String      type;
    private final @Nonnull List<Chart> charts;

    public LogParser(
            @Nonnull String matcher,
            @Nonnull String destination,
            @Nonnull String formatter,
            @Nonnull String type,
            @Nullable List<Chart> charts) {
        this.matcher     = matcher;
        this.destination = destination;
        this.formatter   = formatter;
        this.type        = type;
        this.charts      = Optional
                .ofNullable(charts)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
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

    @Nonnull
    public List<Chart> getCharts() {
        return charts;
    }
}
