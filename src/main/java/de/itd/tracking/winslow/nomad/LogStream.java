package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LogStream {

    private static final Logger LOG = Logger.getLogger(LogStream.class.getSimpleName());

    private BufferedReader reader;

    private LogStream(@Nonnull InputStream inputStream) {
        this(inputStream, StandardCharsets.UTF_8);
    }

    private LogStream(@Nonnull InputStream inputStream, Charset charset) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    @Nullable
    private String getNextLineOmitExceptions() {
        try {
            return getNextLine();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to read line", e);
            return null;
        }
    }

    @Nullable
    private String getNextLine() throws IOException {
        String line = this.reader.readLine();
        if (line == null) {
            this.reader = null;
        }
        return line;
    }

    public static Stream<LogEntry> stdOut(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier) throws IOException {
        return stream(api, taskName, stateSupplier, "stdout").map(LogEntry::stdout);
    }

    public static Stream<LogEntry> stdErr(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier) throws IOException {
        return stream(api, taskName, stateSupplier, "stderr").map(LogEntry::stderr);
    }

    private static Stream<String> stream(
            @Nonnull ClientApi api,
            @Nonnull String taskName,
            @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier,
            @Nonnull String logType) throws IOException {
        var stream = new LogStream(new LogInputStream(api, taskName, stateSupplier, logType, true));
        return Stream.<String>iterate(null, v -> stream.reader != null || v != null, v -> {
            if (stream.reader != null) {
                return stream.getNextLineOmitExceptions();
            } else {
                return null;
            }
        }).filter(Objects::nonNull);
    }
}
