package de.itdesigners.winslow;

import de.itdesigners.winslow.fs.LockBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class Election {

    private final @Nonnull String issuer;
    private final @Nonnull String projectId;
    private final          long   time;
    private final          long   duration;

    private final Map<String, Participation> participations = new TreeMap<>();

    private @Nonnull ResourceAllocationMonitor.ResourceSet<Long> requiredResources = new ResourceAllocationMonitor.ResourceSet<>();

    public Election(@Nonnull String issuer, @Nonnull String projectId, long time, long duration) {
        this.issuer    = issuer;
        this.projectId = projectId;
        this.time      = time;
        this.duration  = duration;
    }

    public boolean isStillRunning() {
        return time + duration + LockBus.DURATION_SURELY_OUT_OF_DATE > System.currentTimeMillis();
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public synchronized void onNewParticipant(@Nonnull String issuer, @Nonnull Participation participation) {
        this.participations.put(issuer, participation);
    }

    public synchronized boolean hasParticipated(@Nonnull String issuer) {
        return this.participations.containsKey(issuer);
    }

    @Nonnull
    public synchronized Optional<String> getMostFittingParticipant() {
        var list = new ArrayList<>(this.participations.entrySet());

        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            list.sort((a, b) -> {
                // first sort by aversion ascending, and within that, sort by affinity decreasing
                var aversion = Float.compare(a.getValue().aversion, b.getValue().aversion);
                if (aversion == 0) {
                    return Float.compare(b.getValue().affinity, a.getValue().affinity);
                } else {
                    return aversion;
                }
            });

            return Optional.of(list.get(0).getKey());
        }
    }

    public synchronized void setRequiredResources(@Nonnull ResourceAllocationMonitor.ResourceSet<Long> requiredResources) {
        this.requiredResources = requiredResources;
    }

    @Nonnull
    public synchronized ResourceAllocationMonitor.ResourceSet<Long> getRequiredResources() {
        return requiredResources;
    }

    public static class Participation {
        final float affinity;
        final float aversion;

        public Participation(float affinity, float aversion) {
            this.affinity = affinity;
            this.aversion = aversion;
        }
    }
}
