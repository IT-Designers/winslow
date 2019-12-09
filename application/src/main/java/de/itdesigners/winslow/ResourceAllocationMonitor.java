package de.itdesigners.winslow;

import de.itdesigners.winslow.config.Requirements;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ResourceAllocationMonitor {

    private final @Nonnull Set<Long>  resources;
    private final @Nonnull Set<Float> affinity;
    private final @Nonnull Set<Float> aversion;

    private @Nonnull Set<Long> reserved;

    public ResourceAllocationMonitor(@Nonnull Set<Long> resources) {
        this(resources, new Set<>(), new Set<>());
    }

    public ResourceAllocationMonitor(
            @Nonnull Set<Long> resources,
            @Nonnull Set<Float> affinity,
            @Nonnull Set<Float> aversion) {
        this.resources = resources;
        this.affinity  = affinity;
        this.aversion  = aversion;

        this.reserved = new Set<>();
    }

    public synchronized void reserve(@Nonnull Set<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            this.reserved.entries.put(
                    entry.getKey(),
                    this.reserved.entries.getOrDefault(entry.getKey(), 0L) + entry.getValue()
            );
        }
    }

    public synchronized void free(@Nonnull Set<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            this.reserved.entries.put(
                    entry.getKey(),
                    this.reserved.entries.getOrDefault(entry.getKey(), entry.getValue()) - entry.getValue()
            );
        }
    }

    public synchronized boolean couldReserveConsideringReservations(@Nonnull Set<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            if (entry.getValue() > getAvailable(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean couldReserveNotConsideringReservations(@Nonnull Set<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            if (entry.getValue() > resources.entries.getOrDefault(entry.getKey(), 0L)) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private Long getAvailable(@Nonnull String resource) {
        return this.resources.entries.getOrDefault(resource, 0L) - this.reserved.entries.getOrDefault(resource, 0L);
    }

    public float getAffinity(@Nonnull Set<Long> resources) {
        var result = 1.f;

        for (Map.Entry<String, Long> entry : resources.entries.entrySet()) {
            var resource  = entry.getKey();
            var required  = entry.getValue();
            var available = getAvailable(resource);
            var affinity  = getAffinity(resource);

            var ratio = required.floatValue() / available.floatValue() * affinity;

            result = Math.min(result, ratio);
        }

        return result;
    }

    @Nonnull
    private Float getAffinity(@Nonnull String resource) {
        return this.affinity.entries.getOrDefault(resource, 1.0f);
    }

    public float getAversion(@Nonnull Set<Long> resources) {
        var result = 0.f;

        for (Map.Entry<String, Long> entry : resources.entries.entrySet()) {
            var resource  = entry.getKey();
            var required  = entry.getValue();
            var available = getAvailable(resource);
            var unused    = (float) (available - required);
            var aversion  = getAversion(resource);

            var ratio = unused / available.floatValue() * aversion;

            result = Math.max(result, ratio);
        }

        return result;
    }

    @Nonnull
    private Float getAversion(@Nonnull String resource) {
        return this.aversion.entries.getOrDefault(resource, 1.0f);
    }

    public enum StandardResources {
        CPU("cpu"),
        RAM("ram"),
        GPU("gpu");

        public final @Nonnull String name;

        StandardResources(@Nonnull String name) {
            this.name = name;
        }
    }

    public static class Set<T> {
        protected final Map<String, T> entries = new HashMap<>();

        public static Set<Long> from(@Nonnull Requirements requirements) {
            return new Set<Long>()
                    .with(StandardResources.RAM, requirements.getMegabytesOfRam() * 1024 * 1024)
                    .with(
                            StandardResources.GPU,
                            requirements.getGpu().map(Requirements.Gpu::getCount).map(Number::longValue).orElse(0L)
                    );
        }

        @Nonnull
        @CheckReturnValue
        public Set<T> with(@Nonnull StandardResources resource, T value) {
            return this.with(resource.name, value);
        }

        @Nonnull
        @CheckReturnValue
        public Set<T> with(@Nonnull String resource, T value) {
            this.entries.put(resource, value);
            return this;
        }

        @Override
        public String toString() {
            var builder = new StringBuilder("Set@{");
            var size    = this.entries.size();

            for (var entry : this.entries.entrySet()) {
                builder.append(entry.getKey())
                       .append("=")
                       .append(entry.getValue());

                if (size > 1) {
                    size -= 1;
                    builder.append(", ");
                }
            }

            return builder.append("}#").append(hashCode()).toString();
        }
    }
}
