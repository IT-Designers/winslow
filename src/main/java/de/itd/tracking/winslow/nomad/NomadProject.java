package de.itd.tracking.winslow.nomad;

import javax.annotation.Nonnull;

public class NomadProject {
    @Nonnull private final String jobId;
    @Nonnull private final String taskName;

    public NomadProject(@Nonnull String jobId, @Nonnull String taskName) {
        this.jobId = jobId;
        this.taskName = taskName;
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
    public Submission toSubmission(@Nonnull NomadOrchestrator orchestrator) {
        return new Submission(orchestrator, jobId, taskName);
    }
}
