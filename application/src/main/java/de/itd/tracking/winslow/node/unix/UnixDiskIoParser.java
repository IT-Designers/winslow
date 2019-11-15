package de.itd.tracking.winslow.node.unix;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UnixDiskIoParser {

    private static final long    UNIX_SECTOR_SIZE     = 512;
    private static final Pattern WHITESPACE_SEPARATOR = Pattern.compile("[ ]+");

    public static Stream<DiskInfo> getDiskInfoConsiderOnlyPhysicalInterfaces(Stream<String> lines) throws IOException {
        // rules from lsblk https://github.com/karelzak/util-linux/blob/master/misc-utils/lsblk.c#L385
        return parseDisks(lines).filter(d -> {
            var name = d.getKey();
            return !(name.startsWith("dm-") || name.startsWith("loop") || name.startsWith("md"));
        }).map(AbstractMap.Entry::getValue);
    }

    private static Stream<Map.Entry<String, DiskInfo>> parseDisks(Stream<String> lines) {
        return lines.map(String::trim).map(WHITESPACE_SEPARATOR::split).map(columns -> {
            // https://www.kernel.org/doc/Documentation/ABI/testing/procfs-diskstats
            //  ~ https://www.kernel.org/doc/Documentation/block/stat.txt
            //  ~ https://www.kernel.org/doc/Documentation/iostats.txt
            var name           = columns[2];
            var sectorsRead    = Long.parseLong(columns[2 + 3]);
            var sectorsWritten = Long.parseLong(columns[2 + 7]);
            var bytesRead      = UNIX_SECTOR_SIZE * sectorsRead;
            var bytesWritten   = UNIX_SECTOR_SIZE * sectorsWritten;
            return new AbstractMap.SimpleEntry<>(name, new DiskInfo(bytesRead, bytesWritten));
        });
    }

    static class DiskInfo {
        private final long bytesRead;
        private final long bytesWritten;

        DiskInfo(long bytesRead, long bytesWritten) {
            this.bytesRead    = bytesRead;
            this.bytesWritten = bytesWritten;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        public long getBytesWritten() {
            return bytesWritten;
        }

        @Nonnull
        public DiskInfo getChangeSince(@Nonnull DiskInfo before) {
            return new DiskInfo(this.bytesRead - before.bytesRead, this.bytesWritten - before.bytesWritten);
        }
    }
}
