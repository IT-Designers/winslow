package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.OrchestratorException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

public class PreparedJob {

    private Job               job;
    private NomadOrchestrator orchestrator;


    public PreparedJob(Job job, NomadOrchestrator orchestrator) {
        this.job = job;
        this.orchestrator = orchestrator;
    }

    @Nonnull
    public Optional<AllocatedJob> start() throws OrchestratorException {
        try {
            if (orchestrator != null && job != null) {
                var jobId      = job.getId();
                var taskName   = job.getTaskGroups().get(0).getName();
                var submission = new AllocatedJob(orchestrator, jobId, taskName);

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
            throw new OrchestratorException("Failed to register job, invalid?", e);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Failed to register job, connection lost?", e);
        }
    }
}
