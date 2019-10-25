package de.itd.tracking.winslow.node.unix;

import de.itd.tracking.winslow.node.MemInfo;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.stream.Stream;

public class UnixMemInfoParser {

    public static final Long   DEFAULT_VALUE      = 0L;
    public static final String SEPARATOR          = ":";
    public static final String SEPARATOR_VALUE    = " ";
    public static final int    MEMINFO_LINE_COUNT = 51;

    @Nonnull private final Stream<String> lines;

    public UnixMemInfoParser(@Nonnull Stream<String> lines) {
        this.lines = lines;
    }

    @Nonnull
    public MemInfo parseMemInfo() {
        var info = new HashMap<String, Long>(MEMINFO_LINE_COUNT);
        lines.map(l -> l.split(SEPARATOR)).forEach(l -> info.put(l[0].trim(), parseValueAsNumberOfBytes(l[1])));

        var systemCache = info.getOrDefault("Cached", DEFAULT_VALUE);
        var memTotal    = info.getOrDefault("MemTotal", DEFAULT_VALUE);
        var memFree     = info.getOrDefault("MemFree", DEFAULT_VALUE) + systemCache;
        var swapTotal   = info.getOrDefault("SwapTotal", DEFAULT_VALUE);
        var swapFree    = info.getOrDefault("SwapFree", DEFAULT_VALUE);

        return new MemInfo(memTotal, memFree, systemCache, swapTotal, swapFree);
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
}
