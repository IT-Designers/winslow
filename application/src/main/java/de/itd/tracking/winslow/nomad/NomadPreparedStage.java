package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.JobsApi;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.OrchestratorConnectionException;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.PreparedStage;
import de.itd.tracking.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class NomadPreparedStage implements PreparedStage {

    private Job                 job;
    private JobsApi             jobsApi;
    private StageDefinition     definition;
    private Stage               stage;
    private String              workspace;
    private Map<String, String> envVariables;
    private Map<String, String> envVariablesPipeline;
    private Map<String, String> envVariablesSystem;
    private Map<String, String> envVariablesInternal;

    public NomadPreparedStage(
            @Nonnull Job job,
            @Nonnull JobsApi jobsApi,
            @Nonnull StageDefinition definition,
            @Nonnull String workspace,
            @Nonnull Map<String, String> envVariables,
            @Nonnull Map<String, String> envVariablesPipeline,
            @Nonnull Map<String, String> envVariablesSystem,
            @Nonnull Map<String, String> envVariablesInternal) {
        this.job                  = job;
        this.jobsApi              = jobsApi;
        this.definition           = definition;
        this.workspace            = workspace;
        this.envVariables         = envVariables;
        this.envVariablesPipeline = envVariablesPipeline;
        this.envVariablesSystem   = envVariablesSystem;
        this.envVariablesInternal = envVariablesInternal;
    }

    @Nonnull
    public Stage execute() throws OrchestratorException {
        try {
            var stage = prepare(Action.Execute);

            if (stage != null) {
                // this one could fail
                jobsApi.register(job);

                // therefore reset those once passed
                this.jobsApi = null;
                this.job     = null;
                this.stage   = stage;
            }

            return this.stage;
        } catch (NomadException e) {
            throw new OrchestratorException("Failed to register job, invalid?", e);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Failed to register job, connection lost?", e);
        }
    }

    @Nonnull
    @Override
    public Stage configure() throws OrchestratorException {
        var stage = prepare(Action.Configure);

        if (stage != null) {
            this.jobsApi = null;
            this.job     = null;
            this.stage   = stage;

            // a configure is successful by being instantiated and has no lifetime
            this.stage.finishNow(Stage.State.Succeeded);
        }

        return this.stage;
    }

    @Nullable
    private Stage prepare(@Nonnull Action action) throws OrchestratorException {
        if (jobsApi != null && job != null) {
            var jobId    = job.getId();
            var taskName = job.getTaskGroups().get(0).getName();

            if (!Objects.equals(jobId, taskName)) {
                throw new OrchestratorException("Invalid configuration, jobId must match taskName, but doesn't: " + jobId + " != " + taskName);
            }

            var stage = new Stage(jobId, definition, action, workspace);
            stage.getEnv().putAll(envVariables);
            stage.getEnvPipeline().putAll(envVariablesPipeline);
            stage.getEnvSystem().putAll(envVariablesSystem);
            stage.getEnvInternal().putAll(envVariablesInternal);

            stage.getEnvPipeline().forEach((key, value) -> stage.getEnv().remove(key, value));
            stage.getEnvSystem().forEach((key, value) -> stage.getEnv().remove(key, value));
            stage.getEnvInternal().forEach((key, value) -> stage.getEnv().remove(key, value));

            return stage;
        } else {
            return null;
        }
    }
}
