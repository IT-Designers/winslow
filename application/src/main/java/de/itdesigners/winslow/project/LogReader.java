package de.itdesigners.winslow.project;

import de.itdesigners.winslow.LogEntry;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.stream.Stream;

public class LogReader implements Iterator<LogEntry> {

    @Nonnull private final SimpleDateFormat dateFormat;
    @Nonnull private final BufferedReader   reader;
    private                String           currentLine;

    private LogReader(@Nonnull InputStream inputStream) {
        this(inputStream, StandardCharsets.UTF_8);
    }

    private LogReader(@Nonnull InputStream inputStream, Charset charset) {
        this.reader     = new BufferedReader(new InputStreamReader(inputStream, charset));
        this.dateFormat = new SimpleDateFormat(LogWriter.DATE_FORMAT);
    }

    @Override
    public synchronized boolean hasNext() {
        try {
            return currentLine != null || (currentLine = reader.readLine()) != null;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized LogEntry next() {
        var split = currentLine.split(LogWriter.LOG_SEPARATOR, 3);
        try {
            var entry = new LogEntry(
                    dateFormat.parse(split[0]).getTime(),
                    split[1].contains("std")
                    ? LogEntry.Source.STANDARD_IO
                    : LogEntry.Source.MANAGEMENT_EVENT,
                    split[1].contains("err"),
                    split[2]
            );
            this.currentLine = null;
            return entry;
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse date", e);
        }
    }

    public static Stream<LogEntry> stream(@Nonnull InputStream inputStream) {
        var reader = new LogReader(inputStream);
        return Stream.<LogEntry>iterate(null, p -> reader.hasNext() || p != null, v -> {
            if (reader.currentLine != null) {
                return reader.next();
            } else {
                return null;
            }
        }).skip(1);
    }
}
