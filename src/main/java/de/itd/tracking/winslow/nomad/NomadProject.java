package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.Submission;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class NomadProject {

    @Nonnull private final List<Entry> history = new ArrayList<>();
    @Nullable private      Entry       current = null;

    @Nonnull
    public Stream<Entry> getHistory() {
        return Stream.concat(history.stream(), getCurrent().stream());
    }

    public void newCurrent(int stageIndex, @Nonnull String jobId, @Nonnull String taskName) {
        this.current = new Entry(stageIndex, jobId, taskName, Submission.State.Preparing);
    }

    public void updateCurrent(@Nonnull Submission.State state) {
        Objects.requireNonNull(this.current);
        this.history.add(this.current);
        this.current = new Entry(this.current.getStageIndex(), this.current.getJobId(), this.current.getTaskName(), state);
    }

    public Optional<Entry> getCurrent() {
        return Optional.ofNullable(this.current);
    }

    public Optional<String> getCurrentJobId() {
        return Optional.ofNullable(this.current).map(Entry::getJobId);
    }

    public Optional<String> getCurrentTaskName() {
        return Optional.ofNullable(this.current).map(Entry::getTaskName);
    }

    public Optional<Submission.State> getCurrentState() {
        return Optional.ofNullable(this.current).map(Entry::getResult);
    }

    @Nonnull
    public Optional<AllocatedJob> getCurrentAllocation(@Nonnull NomadOrchestrator orchestrator) {
        return Optional
                .ofNullable(current)
                .map(c -> new AllocatedJob(orchestrator, c.getJobId(), c.getTaskName(), () -> getHistory().map(entry -> new Submission.HistoryEntry() {
                    @Override
                    public long getTime() {
                        return entry.getTime();
                    }

                    @Override
                    public int getStageIndex() {
                        return entry.getStageIndex();
                    }

                    @Nonnull
                    @Override
                    public Submission.State getState() {
                        return entry.getResult();
                    }

                    @Nonnull
                    @Override
                    public Optional<String> getDescription() {
                        return Optional.of(String.format("JobId=%s, TaskName=%s", entry.getJobId(), entry.getTaskName()));
                    }
                })));
    }

    public static class Entry {
        private final          long             time;
        private final          int              stageIndex;
        @Nonnull private final String           jobId;
        @Nonnull private final String           taskName;
        @Nonnull private final Submission.State result;

        public Entry(int stageIndex, @Nonnull String jobId, @Nonnull String taskName, @Nonnull Submission.State result) {
            this.stageIndex = stageIndex;
            this.time       = System.currentTimeMillis();
            this.jobId      = jobId;
            this.taskName   = taskName;
            this.result     = result;
        }

        public int getStageIndex() {
            return stageIndex;
        }

        public long getTime() {
            return time;
        }

        @Nonnull
        public String getJobId() {
            return jobId;
        }

        @Nonnull
        public String getTaskName() {
            return taskName;
        }

        @Nonnull
        public Submission.State getResult() {
            return result;
        }
    }
}
