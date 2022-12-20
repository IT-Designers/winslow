package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.LogSource;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.Date;

public record LogEntry(
        long time,
        @Nonnull LogSource source,
        boolean error,
        @Nonnull String message) {

    @Nonnull
    @CheckReturnValue
    public static LogEntry stderr(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), LogSource.STANDARD_IO, true, message);
    }

    @Nonnull
    @CheckReturnValue
    public static LogEntry stdout(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), LogSource.STANDARD_IO, false, message);
    }

    @Nonnull
    @CheckReturnValue
    public static LogEntry err(@Nonnull LogSource source, @Nonnull String message) {
        return new LogEntry(new Date().getTime(), source, true, message);
    }

    @Nonnull
    @CheckReturnValue
    public static LogEntry out(@Nonnull LogSource source, @Nonnull String message) {
        return new LogEntry(new Date().getTime(), source, false, message);
    }
}
