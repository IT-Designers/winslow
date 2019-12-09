package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.OrchestratorConnectionException;
import de.itdesigners.winslow.OrchestratorException;
import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.PreparedStage;
import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class NomadPreparedStage implements PreparedStage {

    private NomadBackend        backend;
    private Job                 job;
    private StageDefinition     definition;
    private Result              result;
    private String              workspace;
    private Map<String, String> envVariables;
    private Map<String, String> envVariablesPipeline;
    private Map<String, String> envVariablesSystem;
    private Map<String, String> envVariablesInternal;

    public NomadPreparedStage(
            @Nonnull NomadBackend backend,
            @Nonnull Job job,
            @Nonnull StageDefinition definition,
            @Nonnull String workspace,
            @Nonnull Map<String, String> envVariables,
            @Nonnull Map<String, String> envVariablesPipeline,
            @Nonnull Map<String, String> envVariablesSystem,
            @Nonnull Map<String, String> envVariablesInternal) {
        this.backend              = backend;
        this.job                  = job;
        this.definition           = definition;
        this.workspace            = workspace;
        this.envVariables         = envVariables;
        this.envVariablesPipeline = envVariablesPipeline;
        this.envVariablesSystem   = envVariablesSystem;
        this.envVariablesInternal = envVariablesInternal;
    }

    @Nonnull
    public Result execute() throws OrchestratorException {
        try {
            var stage = prepare(Action.Execute);

            if (stage != null) {
                // this one could fail
                backend.getNewJobsApi().register(job);

                // therefore reset those once passed
                this.job     = null;
                this.result  = new Result(stage, new NomadStageHandle(backend, stage.getId()));
                this.backend = null;
            }

            return this.result;
        } catch (NomadException e) {
            throw new OrchestratorException("Failed to register job, invalid?", e);
        } catch (IOException e) {
            throw new OrchestratorConnectionException("Failed to register job, connection lost?", e);
        }
    }

    @Nonnull
    @Override
    public Result configure() throws OrchestratorException {
        var stage = prepare(Action.Configure);

        if (stage != null) {
            this.job     = null;
            this.result  = new Result(stage, new NomadStageHandle(backend, stage.getId()));
            this.backend = null;

            // a configure is successful by being instantiated and has no lifetime
            this.result.getStage().finishNow(State.Succeeded);
        }

        return this.result;
    }

    @Nullable
    private Stage prepare(@Nonnull Action action) throws OrchestratorException {
        if (backend != null && job != null) {
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
            stage.getEnvSystem().forEach((key, value) -> {
                if (!stage.getEnvPipeline().containsKey(key)) {
                    stage.getEnv().remove(key, value);
                }
            });
            stage.getEnvInternal().forEach((key, value) -> stage.getEnv().remove(key, value));

            return stage;
        } else {
            return null;
        }
    }
}
