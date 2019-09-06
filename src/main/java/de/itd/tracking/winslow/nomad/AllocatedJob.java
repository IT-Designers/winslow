package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.Submission;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AllocatedJob implements Submission {

    private final NomadOrchestrator              orchestrator;
    private final String                         jobId;
    private final String                         taskName;
    private final Supplier<Stream<HistoryEntry>> eventSupplier;

    public AllocatedJob(NomadOrchestrator orchestrator, String jobId, String taskName) {
        this(orchestrator, jobId, taskName, Stream::empty);
    }

    public AllocatedJob(NomadOrchestrator orchestrator, String jobId, String taskName, Supplier<Stream<HistoryEntry>> eventSupplier) {
        this.orchestrator  = orchestrator;
        this.jobId         = jobId;
        this.taskName      = taskName;
        this.eventSupplier = eventSupplier;
    }

    public String getJobId() {
        return jobId;
    }

    public String getTaskName() {
        return taskName;
    }

    @Nonnull
    @Override
    public State getState() throws OrchestratorConnectionException {
        try {
            return orchestrator
                    .getJobAllocationContainingTaskState(jobId, taskName)
                    .flatMap(alloc -> NomadOrchestrator.toRunningStageState(alloc, taskName))
                    .orElse(State.Preparing);
        } catch (NomadException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Failed to connect to nomad", e);
        }
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

    @Override
    public Stream<HistoryEntry> getHistory() {
        return eventSupplier.get();
    }
}
