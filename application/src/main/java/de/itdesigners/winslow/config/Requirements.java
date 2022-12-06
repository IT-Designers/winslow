package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Requirements {

    private final          int     cpu;
    private final @Nonnull Integer ram;
    private final @Nonnull Gpu     gpu;
    private final @Nonnull List<String> tags;

    /**
     * @param cpu            The cpu requirements, nullable to ensure backwards compatibility
     * @param megabytesOfRam Megabytes of RAM to list as requirement
     * @param gpu            Optionally, GPU requirements to list
     */
    public Requirements(
            @Nullable Integer cpu,
            @Nullable Integer megabytesOfRam,
            @Nullable Gpu gpu,
            @Nullable List<String> tags) {
        this.cpu  = cpu != null ? cpu : 0;
        this.ram  = megabytesOfRam != null ? megabytesOfRam : 100;
        this.gpu  = gpu != null ? gpu : new Gpu(null, null, null);
        this.tags = tags != null
                    ? Collections.unmodifiableList(tags)
                    : Collections.emptyList();
    }

    public static Requirements createDefault() {
        return new Requirements(null, null, null, null);
    }

    public int getCpu() {
        return cpu;
    }

    public int getMegabytesOfRam() {
        return ram;
    }

    public Gpu getGpu() {
        return gpu;
    }

    @Nonnull
    public List<String> getTags() {
        return this.tags;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "@{cpu=" + this.cpu
                + ", ram=" + this.ram
                + ", gpu=" + this.gpu
                + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Requirements that = (Requirements) o;
        return ram == that.ram && Objects.equals(gpu, that.gpu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ram, gpu);
    }

    public static class Gpu {
        private final int      count;
        private final @Nonnull String   vendor;
        private final @Nonnull String[] support;

        public Gpu(@Nullable Integer count, @Nullable String vendor, @Nullable String[] support) {
            this.count   = count != null ? count : 0;
            this.vendor  = vendor != null ? vendor : "";
            this.support = support != null ? support : new String[0];
        }


        public int getCount() {
            return count;
        }

        public String getVendor() {
            return vendor;
        }

        public String[] getSupport() {
            return support;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@{count=" + this.count + ", vendor='" + this.vendor + "', support=" + Arrays
                    .toString(this.support) + "}#" + this.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Gpu gpu = (Gpu) o;
            return count == gpu.count && Objects.equals(vendor, gpu.vendor) && Arrays.equals(support, gpu.support);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(count, vendor);
            result = 31 * result + Arrays.hashCode(support);
            return result;
        }
    }
}
