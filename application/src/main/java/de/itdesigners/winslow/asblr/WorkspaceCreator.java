package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Environment;
import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Pipeline;

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

public class WorkspaceCreator implements AssemblerStep {

    private static final Logger LOG = Logger.getLogger(WorkspaceCreator.class.getSimpleName());

    @Nonnull private final Orchestrator orchestrator;
    @Nonnull private final Environment  environment;

    public WorkspaceCreator(@Nonnull Orchestrator orchestrator, @Nonnull Environment environment) {
        this.orchestrator = orchestrator;
        this.environment  = environment;
    }

    @Override
    public boolean applicable(@Nonnull Context context) {
        return context.getEnqueuedStage().getAction() != Action.Configure;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pathOfWorkspace = getWorkspacePathOf(
                context.getPipeline(),
                context.getStageNumber(),
                context.getEnqueuedStage().getDefinition()
        );
        var pathOfPipelineInput  = getPipelineInputPathOf(context.getPipeline());
        var pathOfPipelineOutput = getPipelineOutputPathOf(context.getPipeline());

        {
            var pathOfPipelineLegacyInput  = getPipelineLegacyInputPathOf(context.getPipeline());
            var pathOfPipelineLegacyOutput = getPipelineLegacyOutputPathOf(context.getPipeline());

            upgradePipelineDirectory(context, pathOfPipelineInput, pathOfPipelineLegacyInput, "input");
            upgradePipelineDirectory(context, pathOfPipelineOutput, pathOfPipelineLegacyOutput, "output");
        }

        var workspacesRoot = environment.getResourceManager().getWorkspacesDirectory();
        var resources      = environment.getResourceManager().getResourceDirectory();
        var workspace      = environment.getResourceManager().createWorkspace(pathOfWorkspace, true);
        var pipelineInput  = environment.getResourceManager().createWorkspace(pathOfPipelineInput, false);
        var pipelineOutput = environment.getResourceManager().createWorkspace(pathOfPipelineOutput, false);

        if (workspacesRoot.isEmpty()
                || resources.isEmpty()
                || workspace.isEmpty()
                || pipelineInput.isEmpty()
                || pipelineOutput.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new AssemblyException(
                    "The workspace and resources directory must exit, but at least one isn't."
                            + " workspacesRoot=" + workspacesRoot
                            + ",workspacePath=" + pathOfWorkspace
                            + ",workspace=" + workspace
                            + ",resources=" + resources
                            + ",pipelineInput=" + pipelineInput
                            + ",pipelineOutput=" + pipelineOutput
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


            var workspacesRootDir      = workspacesRoot.get();
            var resourcesAbsolute      = resources.get();
            var workspaceAbsolute      = workspace.get();
            var pipelineInputAbsolute  = pipelineInput.get();
            var pipelineOutputAbsolute = pipelineOutput.get();

            context.store(new WorkspaceConfiguration(
                    workspacesRootDir.relativize(resourcesAbsolute),
                    workspacesRootDir.relativize(workspaceAbsolute),
                    workspacesRootDir.relativize(pipelineInputAbsolute),
                    workspacesRootDir.relativize(pipelineOutputAbsolute),
                    resourcesAbsolute,
                    workspaceAbsolute,
                    pipelineInputAbsolute,
                    pipelineOutputAbsolute
            ));
        }
    }

    private void upgradePipelineDirectory(
            @Nonnull Context context,
            @Nonnull Path latest,
            @Nonnull Path legacy,
            @Nonnull String displayName) {
        if (Files.exists(legacy) && !Files.exists(latest)) {
            context.log(Level.INFO, "Detected legacy pipeline " + displayName + " directory, upgrading...");
            try {
                Files.move(legacy, latest);
                context.log(Level.INFO, "Detected legacy pipeline " + displayName + " directory, upgrading... done");
            } catch (IOException e) {
                context.log(
                        Level.WARNING,
                        "Detected legacy pipeline " + displayName + " directory, upgrading... failed " + e.getMessage()
                );
                LOG.log(Level.WARNING, "Failed to move legacy " + displayName + " directory", e);
            }
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
    public static Path getPipelineInputPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("input");
    }

    @Nonnull
    private static Path getPipelineLegacyInputPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("resources");
    }

    @Nonnull
    public static Path getPipelineOutputPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("output");
    }

    @Nonnull
    private static Path getPipelineLegacyOutputPathOf(@Nonnull Pipeline pipeline) {
        return getProjectWorkspacesDirectory(pipeline.getProjectId()).resolve("unstaged");
    }

    @Nonnull
    private static Path getWorkspacePathOf(@Nonnull Pipeline pipeline, int stageNumber, @Nonnull StageDefinition stage) {
        return getWorkspacePathOf(pipeline.getProjectId(), stageNumber, stage);
    }

    @Nonnull
    public static Path getInitWorkspacePath(@Nonnull String projectId) {
        return getWorkspacePathOf(projectId, 0, (String)null);
    }

    @Nonnull
    public static Path getWorkspacePathOf(@Nonnull String projectId, int stageNumber, @Nonnull StageDefinition stage) {
        return getWorkspacePathOf(projectId, stageNumber, stage.getName());
    }

    @Nonnull
    private static Path getWorkspacePathOf(@Nonnull String projectId, int stageNumber, @Nullable String suffix) {
        return getProjectWorkspacesDirectory(projectId).resolve(
                Orchestrator.replaceInvalidCharactersInJobName(String.format(
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
            this.orchestrator.forcePurgeWorkspace(context.getPipeline().getProjectId(), workspace);
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
                .filter(stage -> stage.getState() == State.Succeeded)
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
