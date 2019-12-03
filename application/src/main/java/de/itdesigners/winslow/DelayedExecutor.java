package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DelayedExecutor {

    private static final Logger LOG = Logger.getLogger(DelayedExecutor.class.getSimpleName());

    private final Map<String, Plan> runnables     = new HashMap<>();
    private final List<Plan>        runnableQueue = new ArrayList<>();

    public DelayedExecutor() {
        var thread = new Thread(this::run);
        thread.setName(DelayedExecutor.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        while (true) {
            Plan planned = null;

            synchronized (this) {
                long sleepFor = 1000;

                if (!this.runnableQueue.isEmpty()) {
                    var now   = System.currentTimeMillis();
                    var index = this.runnableQueue.size() - 1;
                    var plan  = this.runnableQueue.get(index);

                    if (now + 1 >= plan.plannedTimeOfExecution) {
                        this.runnableQueue.remove(index);
                        planned = this.runnables.remove(plan.identifier);
                    } else {
                        sleepFor = plan.plannedTimeOfExecution - now - 1;
                    }
                }
                if (planned == null) {
                    try {
                        this.wait(sleepFor);
                    } catch (InterruptedException e) {
                        LOG.log(Level.WARNING, "Wait got interrupted, might cause performance issues", e);
                    }
                    continue;
                }
            }

            try {
                planned.runnable.run();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Delayed execution failed", t);
            }
        }
    }

    public synchronized boolean executeRandomlyDelayed(
            @Nonnull String identifier,
            long millisDelayMin,
            long millisDelayMax,
            @Nonnull Runnable runnable) {
        if (this.runnables.containsKey(identifier)) {
            return false;
        } else {
            var offset = millisDelayMin + new Random().nextInt((int) (millisDelayMax - millisDelayMin));
            LOG.info("Random offset for " + identifier + " determined to be " + offset + " ms");
            var plan = new Plan(System.currentTimeMillis() + offset, identifier, runnable);
            this.runnables.put(identifier, plan);
            this.runnableQueue.add(plan);
            this.runnableQueue.sort(Comparator.<Plan>comparingLong(planA -> planA.plannedTimeOfExecution).reversed());
            this.notifyAll();
            return true;
        }
    }

    private static class Plan {
        final          long     plannedTimeOfExecution;
        @Nonnull final String   identifier;
        @Nonnull final Runnable runnable;

        private Plan(long plannedTimeOfExecution, @Nonnull String identifier, @Nonnull Runnable runnable) {
            this.plannedTimeOfExecution = plannedTimeOfExecution;
            this.identifier             = identifier;
            this.runnable               = runnable;
        }
    }
}
