package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.Backoff;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LogWriter implements Runnable {

    static final String DATE_FORMAT   = "yyy-MM-dd_HH:mm:ss.SSS";
    static final String LOG_SEPARATOR = " ";

    @Nonnull private final SimpleDateFormat         dateFormat;
    @Nonnull private final LockedOutputStream       outputStream;
    @Nonnull private final Predicate<LogEntry>      predicate;
    @Nonnull private final Supplier<LogEntry>       supplier;
    @Nonnull private final List<Consumer<LogEntry>> consumers;

    private LogWriter(
            @Nonnull LockedOutputStream outputStream,
            @Nonnull Predicate<LogEntry> predicate,
            @Nonnull Supplier<LogEntry> supplier,
            @Nonnull List<Consumer<LogEntry>> consumers) {
        this.outputStream = outputStream;
        this.predicate    = predicate;
        this.supplier     = supplier;
        this.consumers    = consumers;
        this.dateFormat   = new SimpleDateFormat(DATE_FORMAT);
    }

    public static Builder writeTo(@Nonnull LockedOutputStream outputStream) {
        return new Builder(outputStream);
    }


    @Override

    public void run() {
        var      backoff = new Backoff(100, 995, 2.5f);
        LogEntry element = null;

        try (PrintStream ps = new PrintStream(outputStream)) {
            while (predicate.test(element)) {
                element = supplier.get();
                if (element != null) {
                    var stream   = element.isError() ? "err" : "out";
                    var dateTime = dateFormat.format(new Date(element.getTime()));
                    var source   = "std";

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
                    backoff.reset();
                } else {
                    backoff.sleep();
                }
                ps.flush(); // keep output stream alive even if nothing to write
            }
        }
    }

    private void notifyConsumers(LogEntry element) {
        consumers.forEach(c -> c.accept(element));
    }

    public static class Builder {
        @Nonnull final List<Consumer<LogEntry>> additionalConsumer = new ArrayList<>(0);
        @Nonnull final LockedOutputStream       os;
        @Nullable      Predicate<LogEntry>      predicate;
        @Nullable      Supplier<LogEntry>       supplier;

        private Builder(@Nonnull LockedOutputStream os) {
            this.os = os;
        }

        public Builder writeWhile(@Nonnull Predicate<LogEntry> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder fromSupplier(@Nonnull Supplier<LogEntry> supplier) {
            this.supplier = supplier;
            return this;
        }

        public Builder addConsumer(@Nonnull Consumer<LogEntry> consumer) {
            this.additionalConsumer.add(consumer);
            return this;
        }

        public void runInForeground() {
            Objects.requireNonNull(this.predicate);
            Objects.requireNonNull(this.supplier);
            new LogWriter(this.os, this.predicate, this.supplier, this.additionalConsumer).run();
        }
    }
}
