package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Env;

import javax.annotation.Nonnull;
import java.util.Date;

public class InternalEnvironmentVariableAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pipeline = context.getPipeline();
        var definition = context.getEnqueuedStage().getDefinition();
        var timeMs = System.currentTimeMillis();
        var timeS = timeMs / 1_000;
        context.getBuilder()
               .withEnvVariables(definition.getEnvironment())
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
