package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.JobsApi;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.PreparedStage;
import de.itd.tracking.winslow.Stage;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

public class NomadPreparedStage implements PreparedStage {

    private Job             job;
    private JobsApi         jobsApi;
    private StageDefinition definition;
    private Stage           stage;


    public NomadPreparedStage(
            @Nonnull Job job,
            @Nonnull JobsApi jobsApi,
            @Nonnull StageDefinition definition) {
        this.job        = job;
        this.jobsApi    = jobsApi;
        this.definition = definition;
    }

    @Nonnull
    public Stage start() throws OrchestratorException {
        try {
            if (jobsApi != null && job != null) {
                var jobId    = job.getId();
                var taskName = job.getTaskGroups().get(0).getName();

                if (!Objects.equals(jobId, taskName)) {
                    throw new OrchestratorException("Invalid configuration, jobId must match taskName, but doesn't: " + jobId + " != " + taskName);
                }

                var stage = new Stage(jobId, definition, jobId);

                // this one could fail
                jobsApi.register(job);

                // therefore reset those once passed
                this.jobsApi = null;
                this.job     = null;

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
