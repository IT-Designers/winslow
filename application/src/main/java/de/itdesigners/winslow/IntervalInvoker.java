package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class IntervalInvoker {

    public static final long MAYBE_INVOKE_THRESHOLD_MILLIS = 10;

    private final long           interval;
    private final List<Runnable> listeners = new ArrayList<>();

    private long lastUpdate;

    public IntervalInvoker(long interval) {
        this.interval   = interval;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void addListener(@Nonnull Runnable listener) {
        this.listeners.add(listener);
    }

    public long timeMillisUntilNextInvocation() {
        return Math.max(0, (lastUpdate + interval) - System.currentTimeMillis());
    }

    public void maybeInvokeAll() {
        if (timeMillisUntilNextInvocation() < MAYBE_INVOKE_THRESHOLD_MILLIS) {
            this.invokeAll();
        }
    }

    public void invokeAll() {
        this.lastUpdate = System.currentTimeMillis();
        for (var listener : this.listeners) {
            try {
                listener.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
