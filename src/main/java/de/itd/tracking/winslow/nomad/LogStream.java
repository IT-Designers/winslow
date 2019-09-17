package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.javasdk.ClientApi;
import de.itd.tracking.winslow.LogEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LogStream {

    private static final Logger LOG = Logger.getLogger(LogStream.class.getSimpleName());

    private BufferedReader reader;

    private LogStream(@Nonnull LogInputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Nullable
    public String getNextLine() {
        try {
            var line = this.reader.readLine();
            if (line == null) {
                try {
                    this.reader.close();
                } finally {
                    this.reader = null;
                }
            }
            return line;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to read further", e);
            try {
                if (this.reader != null) {
                    this.reader.close();
                }
            } catch (IOException ee) {
                LOG.log(Level.SEVERE, "Failed to close after error", ee);
            } finally {
                this.reader = null;
            }
            return null;
        }
    }


    public static Stream<LogEntry> stdOut(@Nonnull ClientApi api, @Nonnull String taskName, @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier) throws IOException {
        return stream(api, taskName, stateSupplier, "stdout").map(LogEntry::nowOut);
    }

    public static Stream<LogEntry> stdErr(@Nonnull ClientApi api, @Nonnull String taskName, @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier) throws IOException {
        return stream(api, taskName, stateSupplier, "stderr").map(LogEntry::nowErr);
    }

    private static Stream<String> stream(@Nonnull ClientApi api, @Nonnull String taskName, @Nonnull Supplier<Optional<AllocationListStub>> stateSupplier, @Nonnull String logType) throws IOException {
        var stream = new LogStream(new LogInputStream(api, taskName, stateSupplier, logType, true));
        return Stream.<String>iterate(null, v -> stream.reader != null, v -> stream.getNextLine()).filter(Objects::nonNull);
    }
}
