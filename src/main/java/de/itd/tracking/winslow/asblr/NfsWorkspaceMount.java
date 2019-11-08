package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Env;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;

import javax.annotation.Nonnull;

public class NfsWorkspaceMount implements AssemblerStep {

    private static final String TARGET_PATH_RESOURCES = "/resources";
    private static final String TARGET_PATH_WORKSPACE = "/worksapce";

    @Nonnull private final NfsWorkDirectory nfsWorkDirectory;

    public NfsWorkspaceMount(@Nonnull NfsWorkDirectory nfsWorkDirectory) {
        this.nfsWorkDirectory = nfsWorkDirectory;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var config = context.loadOrThrow(WorkspaceConfiguration.class);

        var exportedResources = nfsWorkDirectory.toExportedPath(config.getResourcesDirectory());
        var exportedWorkspace = nfsWorkDirectory.toExportedPath(config.getWorkspaceDirectory());

        if (exportedResources.isEmpty() || exportedWorkspace.isEmpty()) {
            throw new AssemblyException("Failed to retrieve exported path for workspace or resources directory");
        }

        context.getBuilder()
               .addNfsVolume(
                       "winslow-" + context.getStageId() + "-resources",
                       TARGET_PATH_RESOURCES,
                       true,
                       nfsWorkDirectory.getOptions(),
                       exportedResources.get().toAbsolutePath().toString()
               )
               .addNfsVolume(
                       "winslow-" + context.getStageId() + "-workspace",
                       TARGET_PATH_WORKSPACE,
                       false,
                       nfsWorkDirectory.getOptions(),
                       exportedWorkspace.get().toAbsolutePath().toString()
               )
               .withInternalEnvVariable(Env.SELF_PREFIX + "_DIR_RESOURCES", TARGET_PATH_RESOURCES)
               .withDockerImageArguments(Env.SELF_PREFIX + "_DIR_WORKSPACE", TARGET_PATH_WORKSPACE)
               .withWorkspaceWithinPipeline(config.getWorkspaceDirectory().toString());

    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to do
    }
}
