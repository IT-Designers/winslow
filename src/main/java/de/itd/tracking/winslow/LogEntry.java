package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.Date;

public class LogEntry {

    private final long    timeMsSinceEpoch;
    private final boolean error;
    private final String  message;

    public LogEntry(long timeMsSinceEpoch, boolean error, @Nonnull String message) {
        this.timeMsSinceEpoch = timeMsSinceEpoch;
        this.error            = error;
        this.message          = message;
    }

    @Nonnull
    public static LogEntry nowErr(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), true, message);
    }

    @Nonnull
    public static LogEntry nowOut(@Nonnull String message) {
        return new LogEntry(new Date().getTime(), false, message);
    }

    public long getTime() {
        return timeMsSinceEpoch;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
