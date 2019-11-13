package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Env;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class EnvironmentVariableAppender implements AssemblerStep {

    @Nonnull private final Map<String, String> globalEnvironmentVariables;

    public EnvironmentVariableAppender(@Nullable Map<String, String> globalEnvironmentVariables) {
        this.globalEnvironmentVariables = globalEnvironmentVariables != null
                                          ? globalEnvironmentVariables
                                          : Collections.emptyMap();
    }


    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pipeline   = context.getPipeline();
        var definition = context.getEnqueuedStage().getDefinition();
        var timeMs     = System.currentTimeMillis();
        var timeS      = timeMs / 1_000;
        context.getBuilder()
               .withEnvVariables(definition.getEnvironment())
               .withInternalEnvVariables(globalEnvironmentVariables)
               .withInternalEnvVariable(Env.SELF_PREFIX + "_PROJECT_ID", pipeline.getProjectId())
               .withInternalEnvVariable(Env.SELF_PREFIX + "_PIPELINE_ID", pipeline.getProjectId())
               .withInternalEnvVariable(Env.SELF_PREFIX + "_PIPELINE_NAME", context.getPipelineDefinition().getName())
               .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_ID", context.getStageId())
               .withInternalEnvVariable(
                       Env.SELF_PREFIX + "_STAGE_NAME",
                       context.getEnqueuedStage().getDefinition().getName()
               )
               .withInternalEnvVariable(
                       Env.SELF_PREFIX + "_STAGE_NUMBER",
                       Integer.toString(pipeline.getStageCount())
               )
               .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_DATE_TIME", new Date(timeS).toString())
               .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_EPOCH_TIME", Long.toString(timeS))
               .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_EPOCH_TIME_MS", Long.toString(timeMs));
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
