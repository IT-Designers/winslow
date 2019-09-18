package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.Backoff;
import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.fs.LockedOutputStream;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LogWriter implements Runnable {

    static final String DATE_FORMAT   = "yyy-MM-dd_HH:mm:ss.SSS";
    static final String LOG_SEPARATOR = " ";

    @Nonnull private final SimpleDateFormat    dateFormat;
    @Nonnull private final LockedOutputStream  outputStream;
    @Nonnull private final Predicate<LogEntry> predicate;
    @Nonnull private final Supplier<LogEntry>  supplier;

    private LogWriter(@Nonnull LockedOutputStream outputStream, @Nonnull Predicate<LogEntry> predicate, @Nonnull Supplier<LogEntry> supplier) {
        this.outputStream = outputStream;
        this.predicate    = predicate;
        this.supplier     = supplier;
        this.dateFormat   = new SimpleDateFormat(DATE_FORMAT);
    }

    public static void foreground(@Nonnull LockedOutputStream outputStream, @Nonnull Predicate<LogEntry> predicate, @Nonnull Supplier<LogEntry> supplier) {
        new LogWriter(outputStream, predicate, supplier).run();
    }


    @Override
    public void run() {
        var      backoff = new Backoff(100, 1_500, 2.5f);
        LogEntry element = null;

        try (PrintStream ps = new PrintStream(outputStream)) {
            while (predicate.test(element)) {
                element = supplier.get();
                if (element != null) {
                    var stream   = element.isError() ? "stderr" : "stdout";
                    var dateTime = dateFormat.format(new Date(element.getTime()));
                    ps.println(String.join(LOG_SEPARATOR, dateTime, stream, element.getMessage()));
                    backoff.reset();
                } else {
                    backoff.sleep();
                }
                ps.flush(); // keep output stream alive even if nothing to write
            }
        }
    }
}
