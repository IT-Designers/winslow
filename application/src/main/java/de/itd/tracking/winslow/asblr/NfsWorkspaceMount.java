package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Env;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.PreparedStageBuilder;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class NfsWorkspaceMount implements AssemblerStep {

    private static final String TARGET_PATH_RESOURCES          = "/resources";
    private static final String TARGET_PATH_PIPELINE_RESOURCES = "/input";
    private static final String TARGET_PATH_PIPELINE_WORKSPACE = "/workspace";
    private static final String TARGET_PATH_PIPELINE_UNSTAGED  = "/output";

    private static final String ENV_DIR_RESOURCES          = Env.SELF_PREFIX + "_DIR_RESOURCES";
    private static final String ENV_DIR_PIPELINE_RESOURCES = Env.SELF_PREFIX + "_DIR_PIPELINE_RESOURCES";
    private static final String ENV_DIR_PIPELINE_WORKSPACE = Env.SELF_PREFIX + "_DIR_PIPELINE_WORKSPACE";
    private static final String ENV_DIR_PIPELINE_UNSTAGED  = Env.SELF_PREFIX + "_DIR_PIPELINE_UNSTAGED";
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
        var builder = context
                .getBuilder()
                .withWorkspaceDirectory(TARGET_PATH_PIPELINE_WORKSPACE)
                .withInternalEnvVariable(ENV_DIR_WORKSPACE, TARGET_PATH_PIPELINE_WORKSPACE);

        addNfsVolume(
                builder,
                stageId,
                ENV_DIR_RESOURCES,
                config.getResourcesDirectoryAbsolute(),
                TARGET_PATH_RESOURCES,
                true
        );
        addNfsVolume(
                builder,
                stageId,
                ENV_DIR_PIPELINE_RESOURCES,
                config.getPipelineResourcesDirectoryAbsolute(),
                TARGET_PATH_PIPELINE_RESOURCES,
                true
        );
        addNfsVolume(
                builder,
                stageId,
                ENV_DIR_PIPELINE_WORKSPACE,
                config.getWorkspaceDirectoryAbsolute(),
                TARGET_PATH_PIPELINE_WORKSPACE,
                false
        );
        addNfsVolume(
                builder,
                stageId,
                ENV_DIR_PIPELINE_UNSTAGED,
                config.getPipelineUnstagedDirectoryAbsolute(),
                TARGET_PATH_PIPELINE_UNSTAGED,
                false
        );
    }

    private void addNfsVolume(
            @Nonnull PreparedStageBuilder builder,
            @Nonnull String stageId,
            @Nonnull String env,
            @Nonnull Path target,
            @Nonnull String targetFromWithin,
            boolean readonly) throws AssemblyException {
        builder.addNfsVolume(
                stageId + "_" + env,
                targetFromWithin,
                readonly,
                nfsWorkDirectory.getOptions(),
                nfsWorkDirectory
                        .toExportedPath(target)
                        .orElseThrow(() -> new AssemblyException("Failed to retrieve exported path of " + target + " for " + targetFromWithin))
                        .toAbsolutePath()
                        .toString()
        ).withInternalEnvVariable(env, targetFromWithin);
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to do
    }
}
