package de.itd.tracking.winslow.config;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Requirements {
    private final long ram;
    private final Gpu  gpu;

    public Requirements(long ram, Gpu gpu) {
        this.ram = ram;
        this.gpu = gpu;
    }

    public long getMegabytesOfRam() {
        return ram;
    }

    public Optional<Gpu> getGpu() {
        return Optional.ofNullable(gpu);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"@{ram="+this.ram+", gpu="+this.gpu+"}#"+this.hashCode();
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
        private final String   vendor;
        private final String[] support;

        public Gpu(int count, String vendor, String[] support) {
            this.count = count;
            this.vendor = vendor;
            this.support = support;
        }

        public int getCount() {
            return count;
        }

        public Optional<String> getVendor() {
            return Optional.ofNullable(vendor);
        }

        public String[] getSupport() {
            return support;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"@{count="+this.count+", vendor='"+this.vendor+"', support="+ Arrays.toString(this.support) +"}#"+this.hashCode();
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
