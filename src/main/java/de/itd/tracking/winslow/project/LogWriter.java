package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.fs.LockedOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LogWriter implements Runnable {

    static final String DATE_FORMAT   = "yyy-MM-dd_HH:mm:ss.SSS";
    static final String LOG_SEPARATOR = " ";

    @Nonnull private final SimpleDateFormat         dateFormat;
    @Nonnull private final Stream<LogEntry>         streamSource;
    @Nonnull private final LockedOutputStream       outputStream;
    @Nonnull private final List<Consumer<LogEntry>> consumers;

    private LogWriter(
            @Nonnull Stream<LogEntry> streamSource,
            @Nonnull LockedOutputStream target,
            @Nonnull List<Consumer<LogEntry>> consumers) {
        this.streamSource = streamSource;
        this.outputStream = target;
        this.consumers    = consumers;
        this.dateFormat   = new SimpleDateFormat(DATE_FORMAT);
    }

    public static Builder writeTo(@Nonnull LockedOutputStream outputStream) {
        return new Builder(outputStream);
    }

    @Override
    public void run() {
        try (PrintStream ps = new PrintStream(outputStream)) {
            this.streamSource.forEach(element -> {
                if (element != null) {
                    var stream   = element.isError() ? "err" : "out";
                    var dateTime = dateFormat.format(new Date(element.getTime()));
                    var source   = (String)null;

                    switch (element.getSource()) {
                        case STANDARD_IO:
                            source = "std";
                            break;
                        case MANAGEMENT_EVENT:
                            source = "evt";
                            break;
                    }

                    ps.println(String.join(LOG_SEPARATOR, dateTime, source + stream, element.getMessage()));
                    notifyConsumers(element);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void notifyConsumers(LogEntry element) {
        consumers.forEach(c -> c.accept(element));
    }

    public static class Builder {
        @Nonnull final List<Consumer<LogEntry>> additionalConsumer = new ArrayList<>(0);
        @Nonnull final LockedOutputStream       os;
        @Nullable      Stream<LogEntry>         streamSource;

        private Builder(@Nonnull LockedOutputStream os) {
            this.os = os;
        }

        public Builder source(Stream<LogEntry> stream) {
            this.streamSource = stream;
            return this;
        }

        public Builder addConsumer(@Nonnull Consumer<LogEntry> consumer) {
            this.additionalConsumer.add(consumer);
            return this;
        }

        public void runInForeground() {
            Objects.requireNonNull(this.streamSource);
            new LogWriter(this.streamSource, this.os, this.additionalConsumer).run();
        }
    }
}
