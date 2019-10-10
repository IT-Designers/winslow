package de.itd.tracking.winslow.node.unix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnixCpuInfoParser {

    public static final String SEPARATOR = " ";

    @Nonnull private final String[] lines;

    private int cpuCount;

    public UnixCpuInfoParser(@Nonnull String[] lines) {
        this.lines = lines;
        this.cpuCount = parseCpuCount();
    }

    private String getTotalCpuLine() {
        return lines[0];
    }

    private Stream<String> getCpuCoreLines() {
        return Stream.of(lines).skip(1).takeWhile(line -> line.startsWith("cpu"));
    }

    private int parseCpuCount() {
        return Math.max(1, (int) getCpuCoreLines().count());
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public CpuTimes getTotalCpuTimes() {
        return new CpuTimes(getTotalCpuLine().split(SEPARATOR));
    }

    public List<CpuTimes> getCpuTimes() {
        return getCpuCoreLines()
                .map(line -> line.split(SEPARATOR))
                .map(CpuTimes::new)
                .collect(Collectors.toUnmodifiableList());
    }

    public static class CpuTimes {
        private final long user;
        private final long nice;
        private final long system;
        private final long idle;
        private final long iowait;
        private final long irq;
        private final long steal;
        private final long guest;
        private final long guest_nice;

        CpuTimes(@Nonnull String[] line) {
            this(
                    Long.parseLong(line[1]),
                    Long.parseLong(line[2]),
                    Long.parseLong(line[3]),
                    Integer.parseInt(line[4]),
                    Long.parseLong(line[5]),
                    Long.parseLong(line[6]),
                    Long.parseLong(line[7]),
                    Integer.parseInt(line[8]),
                    Long.parseLong(line[9])
            );
        }

        CpuTimes(
                long user,
                long nice,
                long system,
                long idle,
                long iowait,
                long irq,
                long steal,
                long guest,
                long guest_nice) {
            this.user = user;
            this.nice = nice;
            this.system = system;
            this.idle = idle;
            this.iowait = iowait;
            this.irq = irq;
            this.steal = steal;
            this.guest = guest;
            this.guest_nice = guest_nice;
        }

        public long getTimeUnitsUser() {
            return user;
        }

        public long getTimeUnitsNice() {
            return nice;
        }

        public long getTimeUnitsSystem() {
            return system;
        }

        public long getTimeUnitsIdle() {
            return idle;
        }

        public long getTimeUnitsIoWait() {
            return iowait;
        }

        public long getTimeUnitsIrq() {
            return irq;
        }

        public long getTimeUnitsSteal() {
            return steal;
        }

        public long getTimeUnitsGuest() {
            return guest;
        }

        public long getTimeUnitsGuest_nice() {
            return guest_nice;
        }

        public long getTimeUnitsSum() {
            return this.user + this.nice + this.system + this.idle + this.iowait + this.irq + this.steal + this.guest + this.guest_nice;
        }

        public long getTimeUnitsNotIdle() {
            return getTimeUnitsSum() - getTimeUnitsIdle();
        }

        public float getUtilization() {
            var result = Math.min(1, Math.max(0, (float) getTimeUnitsNotIdle() / (float) getTimeUnitsSum()));
            if (Float.isNaN(result)) {
                return 0f;
            } else {
                return result;
            }
        }

        @Nonnull
        public CpuTimes getChangeSince(@Nonnull CpuTimes before) {
            return new CpuTimes(
                    before.user - this.user,
                    before.nice - this.nice,
                    before.system - this.system,
                    before.idle - this.idle,
                    before.iowait - this.iowait,
                    before.irq - this.irq,
                    before.steal - this.steal,
                    before.guest - this.guest,
                    before.guest_nice - this.guest_nice
            );
        }
    }
}
