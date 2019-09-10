package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.io.IOException;

public class PreparedJob {

    private Job               job;
    private NomadOrchestrator orchestrator;
    private NomadStage        stage;


    public PreparedJob(@Nonnull Job job, @Nonnull NomadOrchestrator orchestrator) {
        this.job          = job;
        this.orchestrator = orchestrator;
    }

    @Nonnull
    public NomadStage start(StageDefinition definition) throws OrchestratorException {
        try {
            if (orchestrator != null && job != null) {
                var jobId    = job.getId();
                var taskName = job.getTaskGroups().get(0).getName();
                var stage    = new NomadStage(jobId, taskName, definition);

                // this one could fail
                orchestrator.getClient().getJobsApi().register(job);

                // therefore reset those once passed
                this.orchestrator = null;
                this.job          = null;

                this.stage = stage;
            }
            return this.stage;
        } catch (NomadException e) {
            throw new OrchestratorException("Failed to register job, invalid?", e);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Failed to register job, connection lost?", e);
        }
    }
}
