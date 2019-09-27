package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class UnixMemInfoParser {

    public static final Long   DEFAULT_VALUE   = 0L;
    public static final String SEPARATOR       = ":";
    public static final String SEPARATOR_VALUE = " ";

    @Nonnull private final Stream<String> lines;

    public UnixMemInfoParser(@Nonnull Stream<String> lines) {
        this.lines = lines;
    }

    @Nonnull
    public MemInfo parseMemInfo() {
        var info = new UnixMemInfo();
        lines.map(l -> l.split(SEPARATOR)).forEach(l -> {
            info.insert(l[0].trim(), parseValueAsNumberOfBytes(l[1]));
        });
        return info;
    }

    private static long parseValueAsNumberOfBytes(String s) {
        var  value = s.trim().split(SEPARATOR_VALUE);
        long bytes = Long.parseLong(value[0].trim());

        if (value.length > 1) {
            bytes *= getToBytesMultiplier(value[1]);
        }

        return bytes;
    }

    private static long getToBytesMultiplier(String s) {
        var multiplier = 1L;
        switch (s.trim().toUpperCase().charAt(0)) {
            case 'P': // PETABYTE (suuuuure)
                multiplier *= 1024;
            case 'T': // TERABYTE (boy, you got memory)
                multiplier *= 1024;
            case 'G': // GIGABYTE
                multiplier *= 1024;
            case 'M': // MEGABYTE
                multiplier *= 1024;
            case 'K': // KILOBYTE
                multiplier *= 1024;
            default:
        }
        return multiplier;
    }

    private static class UnixMemInfo implements MemInfo {
        private final Map<String, Long> info = new HashMap<>();

        void insert(String key, Long value) {
            this.info.put(key, value);
        }

        @Override
        public long getMemoryTotal() {
            return info.getOrDefault("MemTotal", DEFAULT_VALUE);
        }

        @Override
        public long getMemoryFree() {
            return info.getOrDefault("MemFree", DEFAULT_VALUE) + getSystemCache();
        }

        @Override
        public long getSystemCache() {
            return info.getOrDefault("Cached", DEFAULT_VALUE);
        }

        @Override
        public long getSwapTotal() {
            return info.getOrDefault("SwapTotal", DEFAULT_VALUE);
        }

        @Override
        public long getSwapFree() {
            return info.getOrDefault("SwapFree", DEFAULT_VALUE);
        }
    }
}
