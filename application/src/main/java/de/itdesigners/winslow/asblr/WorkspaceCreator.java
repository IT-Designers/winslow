package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Environment;
import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration.WorkspaceMode;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.StageId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
                    .getActiveAndPastExecutionGroups()
                    .flatMap(ExecutionGroup::getStages)
                    .filter(s -> Objects.equals(s.getFullyQualifiedId(), stageId))
                    .findFirst()
                    .orElseThrow(() -> new AssemblyException(
                            "Failed to find continue workspace because a stage with the id " + stageId + " was not found"))
                    .getWorkspace()
                    .orElseThrow(() -> new AssemblyException("Failed to continue on workspace of stage " + stageId + " because it has no workspace"));


            workspaceContinuation = Optional.of(Path.of(baseWorkspace));
        }

        var pathOfWorkspace = workspaceContinuation.orElseGet(() -> getWorkspacePathOf(
                context.getStageId(),
                workspaceConfiguration.isSharedWithinGroup(),
                workspaceConfiguration.isNestedWithinGroup() ? getNestedWorkspaceName(context) : null
        ));
        var pathOfPipelineInput  = getPipelineInputPathOf(context.getPipeline());
        var pathOfPipelineOutput = getPipelineOutputPathOf(context.getPipeline());

        {
            var pathOfPipelineLegacyInput  = getPipelineLegacyInputPathOf(context.getPipeline());
            var pathOfPipelineLegacyOutput = getPipelineLegacyOutputPathOf(context.getPipeline());

            upgradePipelineDirectory(context, pathOfPipelineInput, pathOfPipelineLegacyInput, "input");
            upgradePipelineDirectory(context, pathOfPipelineOutput, pathOfPipelineLegacyOutput, "output");
        }

        var workspaceExistedBefore = environment.getResourceManager().existsWorkspace(pathOfWorkspace);
        var workspacesRoot         = environment.getResourceManager().getWorkspacesDirectory();
        var resources              = environment.getResourceManager().getResourceDirectory();
        var workspace = environment.getResourceManager().createWorkspace(
                pathOfWorkspace,
                workspaceMode != WorkspaceMode.CONTINUATION && !workspaceConfiguration.isSharedWithinGroup()
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
            var workspacesRootDir      = workspacesRoot.get();
            var resourcesAbsolute      = resources.get();
            var workspaceAbsolute      = workspace.get();
            var pipelineInputAbsolute  = pipelineInput.get();
            var pipelineOutputAbsolute = pipelineOutput.get();

            // store the configuration first to allow reverts
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

            if (!context.getSubmission().isConfigureOnly()
                    && workspaceMode == WorkspaceMode.INCREMENTAL
                    && (!workspaceConfiguration.isSharedWithinGroup() || !workspaceExistedBefore)) {
                copyContentOfMostRecentlyAndSuccessfullyExecutedStageTo(
                        context,
                        workspace.get()
                );
            }
        }
    }

    @Nonnull
    private String getNestedWorkspaceName(@Nonnull Context context) {
        return EnvironmentVariableAppender.getRangedEnvironmentVariables(
                context.getSubmission().getStageDefinition(),
                context.getExecutionGroup().getRangedValues().orElseGet(Collections::emptyMap)
        );
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
        return getWorkspacePathOf(new StageId(projectId, 0, null, null), true, null);
    }


    @Nonnull
    public static Path getWorkspacePathOf(@Nonnull StageId stageId, boolean sharedWithinGroup, @Nullable String nestedWorkspaceName) {
        var base = getProjectWorkspacesDirectory(stageId.getProjectId());
        if (sharedWithinGroup) {
            return base.resolve(stageId.getExecutionGroupId().getProjectRelative());
        } else if (nestedWorkspaceName != null) {
            return base.resolve(stageId.getExecutionGroupId().getProjectRelative()).resolve(nestedWorkspaceName);
        } else {
            return base.resolve(stageId.getProjectRelative());
        }
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
        var workspaces = pipeline
                .getExecutionHistory()
                .filter(group -> !group.isConfigureOnly())
                .filter(group -> group.getStages().allMatch(stage -> stage.getState() == State.Succeeded))
                .flatMap(group -> group.getStages().flatMap(s -> s.getWorkspace().stream()))
                .collect(Collectors.toList());

        Collections.reverse(workspaces);

        var workDirBefore = workspaces
                .stream()
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
                .findFirst() // get the most recent existing workspace of successful stage
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
                        context.ensureAssemblyHasNotBeenAborted();
                        if (!dirBefore.relativize(path).toString().startsWith(".")) {
                            var file = path.toFile();
                            var dst  = workspaceTarget.resolve(dirBefore.relativize(path));
                            if (file.isDirectory()) {
                                Files.createDirectories(dst);
                            } else {

                                context.log(Level.INFO, "..." + dst.getFileName());
                                copyFileWhile(path, dst, new Supplier<Boolean>() {
                                    final float totalBytes = (float) path.toFile().length();
                                    long lastGetCall = System.currentTimeMillis() - 1_500;
                                    long lastFileLen = 0;

                                    @Override
                                    public Boolean get() {
                                        if (System.currentTimeMillis() - lastGetCall > 2_000) {
                                            var timeDiff = System.currentTimeMillis() - lastGetCall;
                                            lastGetCall = System.currentTimeMillis();

                                            if (dst.toFile().exists()) {
                                                var fileLen  = dst.toFile().length();
                                                var lenDiff  = fileLen - lastFileLen;
                                                var lenPerc  = (int) (((float) fileLen / totalBytes) * 100.0f);
                                                var bytesSec = (int) (((float) lenDiff / (float) timeDiff) * 1000.0f);
                                                lastFileLen = fileLen;

                                                context.log(
                                                        Level.INFO,
                                                        "..." + dst.getFileName() + String.format(
                                                                ", %3d %%, %d bytes/s, %d bytes in total",
                                                                lenPerc,
                                                                bytesSec,
                                                                fileLen
                                                        )
                                                );
                                            }
                                        }
                                        return !context.hasAssemblyBeenAborted();
                                    }
                                });
                            }
                        }
                        return Stream.empty();
                    } catch (AssemblyException e) {
                        return Stream.of(new IOException(e));
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

    private void copyFileWhile(
            @Nonnull Path source,
            @Nonnull Path destination,
            @Nonnull Supplier<Boolean> condition) throws IOException {
        try (var fis = new FileInputStream(source.toFile())) {
            try (var fos = new FileOutputStream(destination.toFile())) {
                while (condition.get()) {
                    var chunk = new byte[512 * 1024]; // TODO dynamic probing, need to support less than 7MiB/s
                    var read  = fis.read(chunk, 0, chunk.length);
                    if (read >= 0) {
                        fos.write(chunk, 0, read);
                        fos.flush(); // this is slower, but reliefs the storage from write stress (like a nfs backend)
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
