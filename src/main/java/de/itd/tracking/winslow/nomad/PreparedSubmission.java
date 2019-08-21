package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.PreparedStage;
import de.itd.tracking.winslow.RunningStage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class PreparedSubmission implements PreparedStage {

    private       Job               job;
    private       NomadOrchestrator orchestrator;
    private final Path              workspaceDirectory;


    public PreparedSubmission(Job job, NomadOrchestrator orchestrator, Path workspaceDirectory) {
        this.job = job;
        this.orchestrator = orchestrator;
        this.workspaceDirectory = workspaceDirectory;
    }

    @Nonnull
    @Override
    public Path getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    @Nonnull
    @Override
    public Optional<RunningStage> start() throws OrchestratorException {
        try {
            if (orchestrator != null && job != null) {
                var jobId    = job.getId();
                var taskName = job.getTaskGroups().get(0).getName();
                var submission = new Submission(orchestrator, jobId, taskName);

                // this one could fail
                orchestrator.getClient().getJobsApi().register(job);

                // therefore reset those once passed
                this.orchestrator = null;
                this.job = null;

                return Optional.of(submission);
            } else {
                return Optional.empty();
            }
        } catch (NomadException e) {
            e.printStackTrace();
            throw new OrchestratorException("Failed to register job, invalid?", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OrchestratorConnectionException("Failed to register job, connection lost?", e);
        }
    }
}
