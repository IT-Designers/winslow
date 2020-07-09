package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Environment;
import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration.WorkspaceMode;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.StageId;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkspaceCreator implements AssemblerStep {

    private static final Logger LOG = Logger.getLogger(WorkspaceCreator.class.getSimpleName());

    private final @Nonnull Orchestrator orchestrator;
    private final @Nonnull Environment  environment;

    public WorkspaceCreator(@Nonnull Orchestrator orchestrator, @Nonnull Environment environment) {
        this.orchestrator = orchestrator;
        this.environment  = environment;
    }

    @Override
    public boolean applicable(@Nonnull Context context) {
        return !context.getSubmission().isConfigureOnly();
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var workspaceConfiguration = context.getSubmission().getWorkspaceConfiguration();
        var workspaceMode          = workspaceConfiguration.getMode();
        context.log(Level.INFO, "WorkspaceConfiguration.WorkspaceMode=" + workspaceMode);

        var workspaceContinuation = Optional.<Path>empty();
        if (workspaceMode == WorkspaceMode.CONTINUATION) {
            var stageId = workspaceConfiguration.getValue().orElse(null);
            var baseWorkspace = context
                    .getPipeline()
                    .getPresentAndPastExecutionGroups()
                    .flatMap(ExecutionGroup::getStages)
                    .filter(s -> Objects.equals(s.getFullyQualifiedId(), stageId))
                    .findFirst()
                    .orElseThrow(() -> new AssemblyException(
                            "Failed to find continue workspace because a stage with the id " + stageId + " was not found"))
                    .getWorkspace()
                    .orElseThrow(() -> new ClassCastException("Failed to continue on workspace of stage " + stageId + " because it has no workspace"));


            workspaceContinuation = Optional.of(Path.of(baseWorkspace));
        }

        var pathOfWorkspace      = workspaceContinuation.orElseGet(() -> getWorkspacePathOf(context.getStageId()));
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
        var workspace = environment.getResourceManager().createWorkspace(
                pathOfWorkspace,
                workspaceMode != WorkspaceMode.CONTINUATION
        );
        var pipelineInput  = environment.getResourceManager().createWorkspace(pathOfPipelineInput, false);
        var pipelineOutput = environment.getResourceManager().createWorkspace(pathOfPipelineOutput, false);

        if (workspacesRoot.isEmpty()
                || resources.isEmpty()
                || workspace.isEmpty()
                || pipelineInput.isEmpty()
                || pipelineOutput.isEmpty()) {
            // do not delete the workspace if it is re-used
            if (workspaceMode != WorkspaceMode.CONTINUATION) {
                workspace.map(Path::toFile).map(File::delete);
            }
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
            if (!context.getSubmission().isConfigureOnly() && workspaceMode == WorkspaceMode.INCREMENTAL) {
                copyContentOfMostRecentlyAndSuccessfullyExecutedStageTo(
                        context,
                        workspace.get()
                );
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
    public static Path getInitWorkspacePath(@Nonnull String projectId) {
        return getWorkspacePathOf(new StageId(projectId, 0, null, null));
    }


    @Nonnull
    public static Path getWorkspacePathOf(@Nonnull StageId stageId) {
        return getProjectWorkspacesDirectory(stageId.getProjectId()).resolve(stageId.getProjectRelative());
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
                .getPresentAndPastExecutionGroups()
                .filter(group -> !group.isConfigureOnly())
                .takeWhile(group -> group.getId__().getGroupNumberWithinProject() < context
                        .getStageId()
                        .getGroupNumberWithinProject())
                .reduce((first, second) -> second) // get the most recent group
                .stream()
                .flatMap(ExecutionGroup::getStages)
                .filter(stage -> stage.getState() == State.Succeeded)
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
