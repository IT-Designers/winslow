package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Environment;
import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.itd.tracking.winslow.Orchestrator.replaceInvalidCharactersInJobName;

public class WorkspaceCreator implements AssemblerStep {

    private static final   Logger      LOG = Logger.getLogger(WorkspaceCreator.class.getSimpleName());
    @Nonnull private final Environment environment;

    public WorkspaceCreator(@Nonnull Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean applicable(@Nonnull Context context) {
        return context.getEnqueuedStage().getAction() != Action.Configure;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pathOfWorkspace = getWorkspacePathOf(
                context.getPipeline(),
                context.getEnqueuedStage().getDefinition()
        );
        var pathOfPipelineResources = getPipelineResourcesPathOf(context.getPipeline());
        var pathOfPipelineUnstaged  = getPipelineUnstagedPathOf(context.getPipeline());

        var workspacesRoot    = environment.getResourceManager().getWorkspacesDirectory();
        var resources         = environment.getResourceManager().getResourceDirectory();
        var workspace         = environment.getResourceManager().createWorkspace(pathOfWorkspace, true);
        var pipelineResources = environment.getResourceManager().createWorkspace(pathOfPipelineResources, false);
        var pipelineUnstaged  = environment.getResourceManager().createWorkspace(pathOfPipelineUnstaged, false);

        if (workspacesRoot.isEmpty()
                || resources.isEmpty()
                || workspace.isEmpty()
                || pipelineResources.isEmpty()
                || pipelineUnstaged.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new AssemblyException(
                    "The workspace and resources directory must exit, but at least one isn't."
                            + " workspacesRoot=" + workspacesRoot
                            + ",workspacePath=" + pathOfWorkspace
                            + ",workspace=" + workspace
                            + ",resources=" + resources
                            + ",pipelineResources=" + pipelineResources
                            + ",pipelineUnstaged=" + pipelineUnstaged
            );
        } else {
            switch (context.getEnqueuedStage().getAction()) {
                case Execute:
                    copyContentOfMostRecentlyAndSuccessfullyExecutedStageTo(
                            context,
                            workspace.get()
                    );
                    break;
                default:
                    LOG.warning("Unexpected Stage Action " + context.getEnqueuedStage().getAction());
                    context.log(Level.WARNING, "Unexpected Stage Action " + context.getEnqueuedStage().getAction());
                case Configure:
                    break;
            }


            var workspacesRootDir         = workspacesRoot.get();
            var resourcesAbsolute         = resources.get();
            var workspaceAbsolute         = workspace.get();
            var pipelineResourcesAbsolute = pipelineResources.get();
            var pipelineUnstagedAbsolute  = pipelineUnstaged.get();

            context.store(new WorkspaceConfiguration(
                    workspacesRootDir.relativize(resourcesAbsolute),
                    workspacesRootDir.relativize(workspaceAbsolute),
                    workspacesRootDir.relativize(pipelineResourcesAbsolute),
                    workspacesRootDir.relativize(pipelineUnstagedAbsolute),
                    resourcesAbsolute,
                    workspaceAbsolute,
                    pipelineResourcesAbsolute,
                    pipelineUnstagedAbsolute
            ));
        }
    }

    @Override
    public void revert(@Nonnull Context context) {
        context
                .load(WorkspaceConfiguration.class)
                .map(WorkspaceConfiguration::getWorkspaceDirectoryAbsolute)
                .ifPresent(path -> forcePurgeWorkspace(context, path));
    }

    @Nonnull
    private static Path getPipelineResourcesPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("resources");
    }

    @Nonnull
    private static Path getPipelineUnstagedPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("unstaged");
    }

    @Nonnull
    private static Path getWorkspacePathOf(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return getWorkspacePathOf(pipeline.getProjectId(), pipeline.getStageCount(), stage.getName());
    }

    @Nonnull
    public static Path getInitWorkspacePath(@Nonnull String projectId) {
        return getWorkspacePathOf(projectId, 0, null);
    }

    @Nonnull
    private static Path getWorkspacePathOf(@Nonnull String projectId, int stageNumber, @Nullable String suffix) {
        return getProjectWorkspacesDirectory(projectId).resolve(
                replaceInvalidCharactersInJobName(String.format(
                        "%04d%s%s",
                        stageNumber,
                        suffix != null ? "_" : "",
                        suffix != null ? suffix : ""
                ))
        );
    }

    @Nonnull
    private static Path getProjectWorkspacesDirectory(@Nonnull String projectId) {
        return Path.of(projectId);
    }

    private void forcePurgeWorkspace(@Nonnull Context context, @Nonnull Path workspace) {
        try {
            Orchestrator.forcePurge(this.environment.getWorkDirectoryConfiguration().getPath(), workspace);
        } catch (IOException e) {
            context.log(Level.SEVERE, "Failed to get rid of workspace directory " + workspace, e);
        }
    }


    private void copyContentOfMostRecentlyAndSuccessfullyExecutedStageTo(
            @Nonnull Context context,
            @Nonnull Path workspaceTarget) throws AssemblyException {

        var pipeline = context.getPipeline();
        var workDirBefore = pipeline
                .getAllStages()
                .filter(stage -> stage.getState() == Stage.State.Succeeded)
                .filter(stage -> stage.getAction() == Action.Execute)
                .flatMap(stage -> stage.getWorkspace().stream())
                .flatMap(workspace -> {
                    var path = environment.getResourceManager().getWorkspace(Path.of(workspace));
                    if (path.isPresent()) {
                        return Stream.of(path.get());
                    } else {
                        context.log(
                                Level.FINE,
                                "Ignoring missing workspace: " + workspace
                        );
                        return Stream.empty();
                    }
                })
                .reduce((first, second) -> second) // get the last successful stage
                .or(() -> environment
                        .getResourceManager()
                        .getWorkspace(getInitWorkspacePath(pipeline.getProjectId())));

        if (workDirBefore.isPresent()) {
            var dirBefore = workDirBefore.get();
            var failure   = Optional.<IOException>empty();

            LOG.info("Copying into workspace " + workspaceTarget + " ...");
            context.log(Level.INFO, "Copying workspace...");
            context.log(Level.INFO, " - Source directory: " + dirBefore.getFileName());
            context.log(Level.INFO, " - Target directory: " + workspaceTarget.getFileName());

            try (var walk = Files.walk(workDirBefore.get())) {
                failure = walk.flatMap(path -> {
                    try {
                        var file = path.toFile();
                        var dst  = workspaceTarget.resolve(dirBefore.relativize(path));
                        if (file.isDirectory()) {
                            Files.createDirectories(dst);
                        } else {
                            Files.copy(path, dst);
                        }
                        return Stream.empty();
                    } catch (IOException e) {
                        return Stream.of(e);
                    }
                }).findFirst();
            } catch (IOException e) {
                failure = Optional.of(e);
            }

            if (failure.isPresent()) {
                throw new AssemblyException("Failed to prepare workspace", failure.get());
            }
        } else {
            context.log(Level.WARNING, "No previous valid workspace directory found");
        }
    }
}
