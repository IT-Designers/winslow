package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.itd.tracking.winslow.Orchestrator.replaceInvalidCharactersInJobName;

public class WorkspaceCreator implements AssemblerStep {

    private static final   Logger          LOG = Logger.getLogger(WorkspaceCreator.class.getSimpleName());
    @Nonnull private final ResourceManager resourceManager;

    public WorkspaceCreator(@Nonnull ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var path = createWorkspacePathFor(
                context.getPipeline(),
                context.getEnqueuedStage().getDefinition()
        );
        var resources = resourceManager.getResourceDirectory();
        var workspace = resourceManager.createWorkspace(path, true);

        if (resources.isEmpty() || workspace.isEmpty()) {
            workspace.map(Path::toFile).map(File::delete);
            throw new AssemblyException(
                    "The workspace and resources directory must exit, but at least one isn't. workspacePath=" + path + ",workspace=" + workspace + ",resources=" + resources
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
            context.store(new WorkspaceConfiguration(resources.get(), workspace.get()));
        }
    }

    @Override
    public void revert(@Nonnull Context context) {
        context
                .load(WorkspaceConfiguration.class)
                .map(WorkspaceConfiguration::getWorkspaceDirectory)
                .ifPresent(path -> forcePurgeWorkspace(context, path));
    }

    private static Path createWorkspacePathFor(@Nonnull Pipeline pipeline, @Nonnull StageDefinition stage) {
        return createWorkspacePathFor(pipeline.getProjectId(), pipeline.getStageCount() + 1, stage.getName());
    }

    private static Path createWorkspaceInitPath(@Nonnull String projectId) {
        return createWorkspacePathFor(projectId, 0, null);
    }

    private static Path createWorkspacePathFor(@Nonnull String projectId, int stageNumber, @Nullable String suffix) {
        return Path.of(
                projectId,
                replaceInvalidCharactersInJobName(String.format(
                        "%04d%s%s",
                        stageNumber,
                        suffix != null ? "_" : "",
                        suffix != null ? suffix : ""
                ))
        );
    }

    private static void forcePurgeWorkspace(@Nonnull Context context, @Nonnull Path workspace) {
        try {
            var maxRetries = 3;
            for (int i = 0; i < maxRetries && workspace.toFile().exists(); ++i) {
                var index = i;
                try (var stream = Files.walk(workspace)) {
                    stream.forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry);
                        } catch (NoSuchFileException ignored) {
                        } catch (IOException e) {
                            if (index + 1 == maxRetries) {
                                context.log(Level.WARNING, "Failed to delete: " + entry, e);
                            }
                        }
                    });
                }
            }
            Files.deleteIfExists(workspace);
        } catch (IOException e) {
            context.log(Level.SEVERE, "Failed to get rid of workspace directory " + workspace, e);
        }
    }


    private void copyContentOfMostRecentlyAndSuccessfullyExecutedStageTo(
            @Nonnull Context context,
            @Nonnull Path workspaceTarget) throws AssemblyException {

        var pipeline = context.getPipeline();
        var workDirBefore = resourceManager
                .getWorkspace(pipeline
                                      .getAllStages()
                                      .filter(stage -> stage.getState() == Stage.State.Succeeded)
                                      .filter(stage -> stage.getAction() == Action.Execute)
                                      .map(stage -> getWorkspacePathForStage(pipeline, stage))
                                      .filter(path -> path.toFile().exists())
                                      .reduce((first, second) -> second) // get the last successful stage
                                      .orElseGet(() -> createWorkspaceInitPath(pipeline.getProjectId())))
                .flatMap(resourceManager::getWorkspace);

        if (workDirBefore.isPresent()) {
            var dirBefore = workDirBefore.get();
            var failure   = Optional.<IOException>empty();

            context.log(
                    Level.INFO,
                    "Source workspace directory: " + resourceManager
                            .getWorkspacesDirectory()
                            .map(dir -> dir.getParent().relativize(workDirBefore.get()))
                            .orElse(workDirBefore.get())
            );

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

    private static Path getWorkspacePathForPipeline(@Nonnull Pipeline pipeline) {
        return Path.of(pipeline.getProjectId());
    }

    private static Path getWorkspacePathForStage(@Nonnull Pipeline pipeline, @Nonnull Stage stage) {
        return getWorkspacePathForPipeline(pipeline).resolve(stage.getWorkspace());
    }

}
