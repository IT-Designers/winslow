package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.util.Date;

public class LogEntry {

    public enum Source {
        STANDARD_IO, MANAGEMENT_EVENT
    }

    private final long    time;
    private final Source  source;
    private final boolean error;
    private final String  message;

    public LogEntry(long timeMs, Source source, boolean error, @Nonnull String message) {
        this.time    = timeMs;
        this.source  = source;
        this.error   = error;
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    @Nonnull
    public Source getSource() {
        return source;
    }

    public boolean isError() {
        return error;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    @Nonnull
    public static LogEntry stderr(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), Source.STANDARD_IO, true, message);
    }

    @Nonnull
    public static LogEntry stdout(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), Source.STANDARD_IO, false, message);
    }

    @Nonnull
    public static LogEntry err(@Nonnull Source source, @Nonnull String message) {
        return new LogEntry(new Date().getTime(), source, true, message);
    }

    @Nonnull
    public static LogEntry out(@Nonnull Source source, @Nonnull String message) {
        return new LogEntry(new Date().getTime(), source, false, message);
    }
}
