package de.itd.tracking.winslow.config;

import java.util.Arrays;
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
    }
}
