package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import de.itdesigners.winslow.LogEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

public class LogStream implements Iterator<String> {

    private static final Logger LOG = Logger.getLogger(LogStream.class.getSimpleName());

    private BufferedReader reader;

    private LogStream(@Nonnull InputStream inputStream) {
        this(inputStream, StandardCharsets.UTF_8);
    }

    private LogStream(@Nonnull InputStream inputStream, Charset charset) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    @Nullable
    private String getNextLine() throws IOException {
        String line = this.reader.readLine();
        if (line == null) {
            LOG.fine("read line is null which means this LogStream reached EOF");
            try {
                this.reader.close();
            } finally {
                this.reader = null;
            }
        }
        return line;
    }

    @Override
    public boolean hasNext() {
        return this.reader != null;
    }

    @Override
    public String next() {
        try {
            if (this.reader.ready()) {
                return getNextLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (this.reader != null) {
                    this.reader.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
            } finally {
                this.reader = null;
            }
        }
        return null;
    }

    public static Iterator<LogEntry> stdOutIter(
            @Nonnull NomadApiClient client,
            @Nonnull NomadStageHandle handle) throws IOException {
        return stdIter(client, handle, "stdout", LogEntry::stdout);
    }

    public static Iterator<LogEntry> stdErrIter(
            @Nonnull NomadApiClient client,
            @Nonnull NomadStageHandle handle) throws IOException {
        return stdIter(client, handle, "stderr", LogEntry::stderr);
    }

    public static Iterator<LogEntry> stdIter(
            @Nonnull NomadApiClient client,
            @Nonnull NomadStageHandle handle,
            @Nonnull String logType,
            @Nonnull Function<String, LogEntry> mapper) throws IOException {
        var iter = LogStream.iter(client, handle, logType);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public LogEntry next() {
                return Optional.ofNullable(iter.next())
                               .map(mapper)
                               .orElse(null);
            }

            @Override
            public String toString() {
                return iter.getClass().getSimpleName() + "@{"
                        + "logType='" + logType + "',"
                        + "hasNext()=" + hasNext()
                        + "}#"
                        + iter.hashCode();
            }
        };
    }

    private static Iterator<String> iter(
            @Nonnull NomadApiClient client,
            @Nonnull NomadStageHandle handle,
            @Nonnull String logType) throws IOException {
        return new LogStream(new LogInputStream(client, handle, logType));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{"
                + "hasNext()=" + hasNext()
                + "}#"
                + hashCode();
    }
}
