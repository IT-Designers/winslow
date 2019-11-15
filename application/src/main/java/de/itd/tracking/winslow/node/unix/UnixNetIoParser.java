package de.itd.tracking.winslow.node.unix;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UnixNetIoParser {


    private static final Pattern WHITESPACE_SEPARATOR = Pattern.compile("[ ]+");

    public static Stream<InterfaceInfo> getNetInfoConsiderOnlyPhysicalInterfaces(Stream<String> lines) throws IOException {
        return parseInterfaces(lines).flatMap(entry -> {
            if (entry.getKey().startsWith("eth") || entry.getKey().startsWith("en")) {
                return Stream.of(entry.getValue());
            } else {
                return Stream.empty();
            }
        });
    }

    private static Stream<Map.Entry<String, InterfaceInfo>> parseInterfaces(Stream<String> lines) {
        return lines.flatMap(line -> {
            var split = line.split(":");
            if (split.length > 1) {
                return Stream.of(new String[][]{split});
            } else {
                return Stream.empty();
            }
        }).map(row -> {
            var name   = row[0].trim();
            var values = WHITESPACE_SEPARATOR.split(row[1].trim());
            return new AbstractMap.SimpleEntry<>(
                    name,
                    new InterfaceInfo(Long.parseLong(values[0]), Long.parseLong(values[8]))
            );
        });
    }

    static class InterfaceInfo {
        final long received;
        final long transmitted;


        InterfaceInfo(long received, long transmitted) {
            this.received    = received;
            this.transmitted = transmitted;
        }

        public long getReceived() {
            return received;
        }

        public long getTransmitted() {
            return transmitted;
        }

        @Nonnull
        InterfaceInfo getChangeSince(@Nonnull InterfaceInfo before) {
            return new InterfaceInfo(this.received - before.received, this.transmitted - before.transmitted);
        }
    }
}
