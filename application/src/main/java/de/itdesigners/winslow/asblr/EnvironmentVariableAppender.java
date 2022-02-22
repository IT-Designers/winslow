package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class EnvironmentVariableAppender implements AssemblerStep {

    @Nonnull private final Map<String, String> globalEnvironmentVariables;

    public EnvironmentVariableAppender(@Nullable Map<String, String> globalEnvironmentVariables) {
        this.globalEnvironmentVariables = globalEnvironmentVariables != null
                                          ? globalEnvironmentVariables
                                          : Collections.emptyMap();
    }


    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pipeline = context.getPipeline();
        var pipelineDefinition = context.getPipelineDefinition();
        var stageDefinition = context.getSubmission().getStageDefinition();
        var timeMs = System.currentTimeMillis();
        var timeS = timeMs / 1_000;

        var submission = context
                .getSubmission()
                .withStageEnvVariables(stageDefinition.getEnvironment())
                .withSystemEnvVariables(globalEnvironmentVariables)
                .withPipelineEnvVariables(pipelineDefinition.getEnvironment())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_PROJECT_ID", pipeline.getProjectId())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_PIPELINE_ID", pipeline.getProjectId())
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_PIPELINE_NAME",
                        pipelineDefinition.getName()
                )
                .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_ID", context.getStageId().getFullyQualified())
                .withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_NAME", stageDefinition.getName())
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_SETUP_DATE_TIME",
                        new Date(timeMs).toString()
                )
                .withInternalEnvVariable(Env.SELF_PREFIX + "_SETUP_EPOCH_TIME", Long.toString(timeS))
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_SETUP_EPOCH_TIME_MS",
                        Long.toString(timeMs)
                );

        submission
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_MULTI_STAGE_GROUP",
                        String.valueOf(context.getExecutionGroup().getExpectedGroupSize() > 1)
                )
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_WORKSPACE_SHARED_WITHIN_GROUP",
                        String.valueOf(submission.getWorkspaceConfiguration().isSharedWithinGroup())
                )
                .withInternalEnvVariable(
                        Env.SELF_PREFIX + "_WORKSPACE_NESTED_WITHIN_GROUP",
                        String.valueOf(submission.getWorkspaceConfiguration().isNestedWithinGroup())
                );
        submission.getId().getStageNumberWithinGroup().ifPresent(number -> {
            submission.withInternalEnvVariable(Env.SELF_PREFIX + "_STAGE_NUMBER_WITHIN_GROUP", String.valueOf(number));
        });


        stageDefinition.getRequirements().ifPresent(requirements -> {
            var sub = context
                    .getSubmission()
                    .withInternalEnvVariable(Env.SELF_PREFIX + "_RES_CORES", String.valueOf(requirements.getCpu()))
                    .withInternalEnvVariable(
                            Env.SELF_PREFIX + "_RES_RAM_MB",
                            String.valueOf(requirements.getMegabytesOfRam())
                    )
                    .withInternalEnvVariable(
                            Env.SELF_PREFIX + "_RES_RAM_GB",
                            String.valueOf(requirements.getMegabytesOfRam() / 1024)
                    );
            requirements.getGpu().ifPresent(gpu -> {
                var s = sub.withInternalEnvVariable(Env.SELF_PREFIX + "_RES_GPU_COUNT", String.valueOf(gpu.getCount()));
                gpu.getVendor().ifPresent(vendor -> {
                    var ss = s.withInternalEnvVariable(Env.SELF_PREFIX + "_RES_GPU_VENDOR", vendor);
                });
            });
        });

        context.getExecutionGroup().getRangedValues().ifPresent(ranged -> {
            context.getSubmission()
                   .withInternalEnvVariable(
                           Env.SELF_PREFIX + "_RANGED_ENV_VARIABLE_NAMES",
                           String.join(";", ranged.keySet())
                   )
                   .withInternalEnvVariable(
                           Env.SELF_PREFIX + "_RANGED_ENV_VARIABLES",
                           getRangedEnvironmentVariables(context.getSubmission().getStageDefinition(), ranged)
                   );
        });

        context
                .getExecutionGroup()
                .getComment()
                .ifPresent(comment -> submission.withInternalEnvVariable(Env.SELF_PREFIX + "_GROUP_COMMENT", comment));
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }

    @Nonnull
    public static String getRangedEnvironmentVariables(@Nonnull StageDefinition stageDefinition, @Nonnull Map<String, RangedValue> ranged) {
        return ranged
                .keySet()
                .stream()
                .map(key -> key + ":" + stageDefinition.getEnvironment().get(key))
                .collect(Collectors.joining(";"));
    }
}
