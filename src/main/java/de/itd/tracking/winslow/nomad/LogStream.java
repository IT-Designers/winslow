package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.javasdk.ClientApi;
import de.itd.tracking.winslow.LogEntry;

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
            LOG.info("read line is null");
            this.reader = null;
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
        }
        return null;
    }

    public static Iterator<LogEntry> stdOutIter(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull NomadBackend backend) throws IOException {
        return stdIter(api, taskName, backend, "stdout", LogEntry::stdout);
    }

    public static Iterator<LogEntry> stdErrIter(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull NomadBackend backend) throws IOException {
        return stdIter(api, taskName, backend, "stderr", LogEntry::stderr);
    }

    public static Iterator<LogEntry> stdIter(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull NomadBackend backend,
            @Nonnull String logType,
            @Nonnull Function<String, LogEntry> mapper) throws IOException {
        var iter = LogStream.iter(api, taskName, backend, logType);
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
                return getClass().getSimpleName() + "@{logType='" + logType + "'}#" + hashCode();
            }
        };
    }

    private static Iterator<String> iter(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull NomadBackend backend,
            @Nonnull String logType) throws IOException {
        return new LogStream(new LogInputStream(api, taskName, backend, logType));
    }
}
