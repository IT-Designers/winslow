package de.itdesigners.winslow.project;

import de.itdesigners.winslow.LogEntry;
import de.itdesigners.winslow.fs.LockedOutputStream;

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

    static final @Nonnull String DATE_FORMAT             = "yyy-MM-dd_HH:mm:ss.SSS";
    static final @Nonnull String LOG_SEPARATOR           = " ";
    static final @Nonnull String TRIPLE_STANDARD_IO      = "std";
    static final @Nonnull String TRIPLE_MANAGEMENT_EVENT = "evt";

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
                    var stream   = element.error() ? "err" : "out";
                    var dateTime = dateFormat.format(new Date(element.time()));
                    var source = switch (element.source()) {
                        case STANDARD_IO -> TRIPLE_STANDARD_IO;
                        case MANAGEMENT_EVENT -> TRIPLE_MANAGEMENT_EVENT;
                    };

                    ps.println(String.join(LOG_SEPARATOR, dateTime, source + stream, element.message()));
                    ps.flush();
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
        @Nonnull final List<Consumer<LogEntry>> additionalConsumer = new ArrayList<>();
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
