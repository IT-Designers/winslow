package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.fs.NfsWorkDirectory;
import de.itdesigners.winslow.pipeline.DockerNfsVolume;
import de.itdesigners.winslow.pipeline.DockerNfsVolumes;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;

public class NfsWorkspaceMount implements AssemblerStep {

    private static final String TARGET_PATH_RESOURCES          = "/resources";
    private static final String TARGET_PATH_PIPELINE_INPUT     = "/input";
    private static final String TARGET_PATH_PIPELINE_WORKSPACE = "/workspace";
    private static final String TARGET_PATH_PIPELINE_OUTPUT    = "/output";

    private static final String ENV_DIR_RESOURCES          = Env.SELF_PREFIX + "_DIR_RESOURCES";
    private static final String ENV_DIR_PIPELINE_INPUT     = Env.SELF_PREFIX + "_DIR_INPUT";
    private static final String ENV_DIR_PIPELINE_WORKSPACE = Env.SELF_PREFIX + "_DIR_WORKSPACE";
    private static final String ENV_DIR_PIPELINE_OUTPUT    = Env.SELF_PREFIX + "_DIR_OUTPUT";
    private static final String ENV_DIR_WORKSPACE          = Env.SELF_PREFIX + "_DIR_WORKSPACE";

    @Nonnull private final NfsWorkDirectory nfsWorkDirectory;

    public NfsWorkspaceMount(@Nonnull NfsWorkDirectory nfsWorkDirectory) {
        this.nfsWorkDirectory = nfsWorkDirectory;
    }

    @Override
    public boolean applicable(@Nonnull Context context) {
        return context.getEnqueuedStage().getAction() != Action.Configure;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var config  = context.loadOrThrow(WorkspaceConfiguration.class);
        var stageId = context.getStageId();
        var submission = context
                .getSubmission()
                .withWorkspaceDirectory(config.getWorkspaceDirectory().toString())
                .withInternalEnvVariable(ENV_DIR_WORKSPACE, TARGET_PATH_PIPELINE_WORKSPACE);

        submission = submission.withExtension(new DockerNfsVolumes(List.of(
                volume(
                        submission,
                        stageId,
                        ENV_DIR_RESOURCES,
                        config.getResourcesDirectoryAbsolute(),
                        TARGET_PATH_RESOURCES,
                        true
                ),
                volume(
                        submission,
                        stageId,
                        ENV_DIR_PIPELINE_INPUT,
                        config.getPipelineInputDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_INPUT,
                        true
                ),
                volume(
                        submission,
                        stageId,
                        ENV_DIR_PIPELINE_WORKSPACE,
                        config.getWorkspaceDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_WORKSPACE,
                        false
                ),
                volume(
                        submission,
                        stageId,
                        ENV_DIR_PIPELINE_OUTPUT,
                        config.getPipelineOutputDirectoryAbsolute(),
                        TARGET_PATH_PIPELINE_OUTPUT,
                        false
                )
        )));
    }

    @Nonnull
    @CheckReturnValue
    private DockerNfsVolume volume(
            @Nonnull Submission submission,
            @Nonnull String stageId,
            @Nonnull String env,
            @Nonnull Path target,
            @Nonnull String targetFromWithin,
            boolean readonly) throws AssemblyException {
        submission = submission.withInternalEnvVariable(env, targetFromWithin);
        return new DockerNfsVolume(
                stageId + "_" + env,
                targetFromWithin,
                nfsWorkDirectory
                        .toExportedPath(target)
                        .orElseThrow(() -> new AssemblyException("Failed to retrieve exported path of " + target + " for " + targetFromWithin))
                        .toAbsolutePath()
                        .toString(),
                nfsWorkDirectory.getOptions(),
                readonly
        );
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to do
    }
}
