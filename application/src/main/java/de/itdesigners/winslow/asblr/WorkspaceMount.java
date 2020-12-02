package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.pipeline.DockerVolume;
import de.itdesigners.winslow.pipeline.DockerVolumes;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;

public class WorkspaceMount implements AssemblerStep {

    private static final String TARGET_PATH_RESOURCES          = "/resources";
    private static final String TARGET_PATH_PIPELINE_INPUT     = "/input";
    private static final String TARGET_PATH_PIPELINE_WORKSPACE = "/workspace";
    private static final String TARGET_PATH_PIPELINE_OUTPUT    = "/output";

    private static final String ENV_DIR_RESOURCES          = Env.SELF_PREFIX + "_DIR_RESOURCES";
    private static final String ENV_DIR_PIPELINE_INPUT     = Env.SELF_PREFIX + "_DIR_INPUT";
    private static final String ENV_DIR_PIPELINE_WORKSPACE = Env.SELF_PREFIX + "_DIR_WORKSPACE";
    private static final String ENV_DIR_PIPELINE_OUTPUT    = Env.SELF_PREFIX + "_DIR_OUTPUT";
    private static final String ENV_DIR_WORKSPACE          = Env.SELF_PREFIX + "_DIR_WORKSPACE";

    @Nonnull private final WorkDirectoryConfiguration workDirConf;

    public WorkspaceMount(@Nonnull WorkDirectoryConfiguration workDirConf) {
        this.workDirConf = workDirConf;
    }

    @Override
    public boolean applicable(@Nonnull Context context) {
        return !context.getSubmission().isConfigureOnly();
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var config  = context.loadOrThrow(WorkspaceConfiguration.class);
        var stageId = context.getStageId();
        var submission = context
                .getSubmission()
                .withWorkspaceDirectory(config.getWorkspaceDirectory().toString())
                .withInternalEnvVariable(ENV_DIR_WORKSPACE, TARGET_PATH_PIPELINE_WORKSPACE);

        submission = submission.withExtension(new DockerVolumes(List.of(
                volume(
                        submission,
                        stageId.getFullyQualified(),
                        ENV_DIR_RESOURCES,
                        config.getResourcesDirectoryAbsolute(),
                        TARGET_PATH_RESOURCES,
                        true
                ),
                volume(
                        submission,
                        stageId.getFullyQualified(),
                        ENV_DIR_PIPELINE_INPUT,
                        config.getPipelineInputDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_INPUT,
                        true
                ),
                volume(
                        submission,
                        stageId.getFullyQualified(),
                        ENV_DIR_PIPELINE_WORKSPACE,
                        config.getWorkspaceDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_WORKSPACE,
                        false
                ),
                volume(
                        submission,
                        stageId.getFullyQualified(),
                        ENV_DIR_PIPELINE_OUTPUT,
                        config.getPipelineOutputDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_OUTPUT,
                        false
                )
        )));
    }

    @Nonnull
    @CheckReturnValue
    private DockerVolume volume(
            @Nonnull Submission submission,
            @Nonnull String stageId,
            @Nonnull String env,
            @Nonnull Path target,
            @Nonnull String targetFromWithin,
            boolean readonly) throws AssemblyException {
        submission.withInternalEnvVariable(env, targetFromWithin);
        var config = this.workDirConf
                .getDockerVolumeConfiguration(target)
                .orElseThrow(() -> new AssemblyException("Failed to retrieve volume configuration for target=" + targetFromWithin));
        return new DockerVolume(
                stageId + "_" + env,
                config.getType(),
                targetFromWithin,
                config.getTargetPath(),
                config.getOptions().orElse(""),
                readonly
        );
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to do
    }
}
