package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.RunningStage;

import java.io.IOException;
import java.util.Optional;

public class Submission implements RunningStage {

    private final NomadOrchestrator orchestrator;
    private final String            jobId;
    private final String            taskName;

    public Submission(NomadOrchestrator orchestrator, String jobId, String taskName) {
        this.orchestrator = orchestrator;
        this.jobId = jobId;
        this.taskName = taskName;
    }

    public String getJobId() {
        return jobId;
    }

    public String getTaskName() {
        return taskName;
    }

    @Override
    public Iterable<String> getStdOut(int lastNLines) {
        return () -> new LogIterator(jobId, taskName, "stdout", orchestrator.getClientApi(), () -> {
            try {
                return orchestrator.getJobAllocationContainingTaskState(jobId, taskName);
            } catch (IOException | NomadException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Iterable<String> getStdErr(int lastNLines) {
        return () -> new LogIterator(jobId, taskName, "stderr", orchestrator.getClientApi(), () -> {
            try {
                return orchestrator.getJobAllocationContainingTaskState(jobId, taskName);
            } catch (IOException | NomadException e) {
                return Optional.empty();
            }
        });
    }
}
