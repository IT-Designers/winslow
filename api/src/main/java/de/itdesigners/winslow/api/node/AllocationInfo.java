package de.itdesigners.winslow.api.node;

import javax.annotation.Nonnull;
import java.util.List;

public class AllocationInfo {

    private final @Nonnull List<Allocation> allocations;

    public AllocationInfo(@Nonnull List<Allocation> allocations) {
        this.allocations = allocations;
    }

    @Nonnull
    public List<Allocation> getAllocations() {
        return allocations;
    }

    public static class Allocation {
        private final @Nonnull String title;
        private final          int    cpu;
        private final          int    memory;
        private final          int    gpu;

        public Allocation(@Nonnull String title, int cpu, int memory, int gpu) {
            this.title  = title;
            this.cpu    = cpu;
            this.memory = memory;
            this.gpu    = gpu;
        }

        @Nonnull
        public String getTitle() {
            return title;
        }

        public int getCpu() {
            return cpu;
        }

        public int getMemory() {
            return memory;
        }

        public int getGpu() {
            return gpu;
        }

    }
}
