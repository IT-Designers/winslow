package de.itdesigners.winslow;

import de.itdesigners.winslow.config.Requirements;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ResourceAllocationMonitor {

    private static final ResourceSet<Long> MINIMUM = new ResourceSet<Long>()
            .with(StandardResources.CPU, 1L)
            .with(StandardResources.RAM, 300L * 1024L * 1024L);

    private final @Nonnull ResourceSet<Long>  resources;
    private final @Nonnull ResourceSet<Float> affinity;
    private final @Nonnull ResourceSet<Float> aversion;

    private @Nonnull Map<String, ResourceSet<Long>> reserved;

    public ResourceAllocationMonitor(@Nonnull ResourceSet<Long> resources) {
        this(resources, defaultAffinity(resources), defaultAversion(resources));
    }

    public ResourceAllocationMonitor(
            @Nonnull ResourceSet<Long> resources,
            @Nonnull ResourceSet<Float> affinity,
            @Nonnull ResourceSet<Float> aversion) {
        this.resources = resources;
        this.affinity  = affinity;
        this.aversion  = aversion;

        this.reserved = new HashMap<>();
    }

    public synchronized void reserve(@Nonnull String token, @Nonnull ResourceSet<Long> set) {
        var reserved = this.reserved.computeIfAbsent(token, p -> new ResourceSet<>());
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            reserved.entries.put(
                    entry.getKey(),
                    reserved.getOrDefault(entry.getKey(), 0L) + entry.getValue()
            );
        }
    }

    public synchronized void free(@Nonnull String token, @Nonnull ResourceSet<Long> set) {
        var reserved = this.reserved.computeIfAbsent(token, p -> new ResourceSet<>());
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            reserved.entries.put(
                    entry.getKey(),
                    Math.max(0, reserved.getOrDefault(entry.getKey(), entry.getValue()) - entry.getValue())
            );
        }
        if (set.entries.values().stream().allMatch(v -> v == 0)) {
            this.reserved.remove(token);
        }
    }

    public synchronized boolean couldReserveConsideringReservations(@Nonnull ResourceSet<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            if (entry.getValue() > getAvailable(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean couldReserveNotConsideringReservations(@Nonnull ResourceSet<Long> set) {
        for (Map.Entry<String, Long> entry : set.entries.entrySet()) {
            if (entry.getValue() > resources.getOrDefault(entry.getKey(), 0L)) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private Long getAvailable(@Nonnull String resource) {
        return this.resources.getOrDefault(resource, 0L)
                - this.reserved.values()
                               .stream()
                               .mapToLong(v -> v.getOrDefault(resource, 0L))
                               .sum();
    }

    public float getAffinity(@Nonnull ResourceSet<Long> resources) {
        var result = 1.f;

        var combinedKeys = new HashSet<String>();
        combinedKeys.addAll(resources.entries.keySet());
        combinedKeys.addAll(this.resources.entries.keySet());

        for (var resource : combinedKeys) {
            var available = this.resources.getOrDefault(resource, 0L);
            var required  = getResourceRequirement(resources, resource);

            // skip if not available
            if (available > 0L) {
                var affinity = getAffinity(resource);
                var ratio    = required.floatValue() / available.floatValue() * affinity;

                result = Math.min(result, ratio);
            } else if (required > 0L) {
                return 0f; // insufficient resources
            }
        }

        return result;
    }

    private Long getResourceRequirement(
            @Nonnull ResourceSet<Long> resources,
            @Nonnull String resource) {
        var required = resources.getOrDefault(resource, 0L);
        var minimum  = MINIMUM.getOrDefault(resource, 0L);
        return Math.max(required, minimum);
    }

    @Nonnull
    private Float getAffinity(@Nonnull String resource) {
        return this.affinity.getOrDefault(resource, 1.0f);
    }

    public float getAversion(@Nonnull ResourceSet<Long> resources) {
        var result = 0.f;

        var combinedKeys = new HashSet<String>();
        combinedKeys.addAll(resources.entries.keySet());
        combinedKeys.addAll(this.resources.entries.keySet());

        for (var resource : combinedKeys) {
            var available = this.resources.getOrDefault(resource, 0L);
            var required  = getResourceRequirement(resources, resource);

            if (available > 0L) {
                var unused   = (float) (available - required);
                var aversion = getAversion(resource);

                var ratio = unused / available.floatValue() * aversion;

                result = Math.max(result, ratio);
            } else if (required > 0L) {
                return 1f; // insufficient resources
            }
        }

        return result;
    }

    @Nonnull
    private Float getAversion(@Nonnull String resource) {
        return this.aversion.getOrDefault(resource, 1.0f);
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

    public static class ResourceSet<T> {
        protected final Map<String, T> entries = new HashMap<>();

        @Nonnull
        @CheckReturnValue
        public T getOrDefault(@Nonnull StandardResources resources, T defaultValue) {
            return this.getOrDefault(resources.name, defaultValue);
        }

        @Nonnull
        @CheckReturnValue
        public T getOrDefault(@Nonnull String name, T defaultValue) {
            return this.entries.getOrDefault(name, defaultValue);
        }

        @Nonnull
        @CheckReturnValue
        public ResourceSet<T> with(@Nonnull StandardResources resource, T value) {
            return this.with(resource.name, value);
        }

        @Nonnull
        @CheckReturnValue
        public ResourceSet<T> with(@Nonnull String resource, T value) {
            this.entries.put(resource, value);
            return this;
        }

        @Override
        public String toString() {
            var builder = new StringBuilder(getClass().getSimpleName());
            var size    = this.entries.size();

            builder.append("@{");

            for (var entry : this.entries.entrySet()) {
                builder.append(entry.getKey())
                       .append("=")
                       .append(entry.getValue());

                if (size > 1) {
                    size -= 1;
                    builder.append(", ");
                }
            }

            return builder
                    .append("}#")
                    .append(hashCode())
                    .toString();
        }
    }

    @Nonnull
    private static ResourceSet<Float> defaultAffinity(@Nonnull ResourceSet<Long> resources) {
        var set = new ResourceSet<Float>();
        resources.entries.keySet().forEach(key -> set.entries.put(key, 1.0f));
        return set;
    }

    @Nonnull
    private static ResourceSet<Float> defaultAversion(@Nonnull ResourceSet<Long> resources) {
        var set = new ResourceSet<Float>();
        resources.entries.keySet().forEach(key -> set.entries.put(key, 1.0f));
        if (set.entries.containsKey(StandardResources.GPU.name)) {
            var clone           = new HashMap<>(set.entries);
            var gpuCount        = resources.entries.get(StandardResources.GPU.name);
            var gpuFactor       = set.entries.get(StandardResources.GPU.name);
            var gpuAdjustFactor = gpuFactor / (gpuCount + 1.0f); // this ensures that the GPU has the greatest impact

            for (var entry : clone.entrySet()) {
                if (!entry.getKey().equals(StandardResources.GPU.name)) {
                    set.entries.put(entry.getKey(), entry.getValue() * gpuAdjustFactor);
                }
            }
        }
        return set;
    }
}
