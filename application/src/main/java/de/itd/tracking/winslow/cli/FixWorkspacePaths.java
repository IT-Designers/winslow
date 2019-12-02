package de.itd.tracking.winslow.cli;

import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.asblr.WorkspaceCreator;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.project.ProjectRepository;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FixWorkspacePaths {

    private static final String WRONG_STATIC_PATH_LITERAL_USED = "/workspace";

    private @Nullable List<String>                      projectIds  = null;
    private @Nullable List<String>                      loadFailed  = null;
    private @Nullable List<Project>                     fixablePath = null;
    private @Nullable List<Project>                     fixFailed   = null;
    private @Nullable Map<Project, Map<String, String>> fixedPaths  = null;

    @Nonnull
    @CheckReturnValue
    public FixWorkspacePaths withProjectIds(@Nonnull List<String> projectIds) {
        this.projectIds = projectIds;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public FixWorkspacePaths searchForProjectsWithFixableWorkspaces(
            @Nonnull ProjectRepository repository,
            @Nonnull Orchestrator orchestrator) {
        Objects.requireNonNull(this.projectIds);

        this.loadFailed  = new ArrayList<>(this.projectIds.size());
        this.fixablePath = new ArrayList<>(this.projectIds.size());

        for (var projectId : projectIds) {
            var handle = repository.getProject(projectId);
            var unsafe = handle.unsafe();

            if (unsafe.isEmpty()) {
                this.loadFailed.add(projectId);
            } else {
                var project  = unsafe.get();
                var pipeline = orchestrator.getPipeline(project);

                if (pipeline.isEmpty()) {
                    this.loadFailed.add(projectId);
                } else if (hasFixableWorkspacePath(pipeline.get())) {
                    this.fixablePath.add(unsafe.get());
                }
            }
        }
        return this;
    }

    private boolean hasFixableWorkspacePath(@Nonnull Pipeline pipeline) {
        return pipeline
                .getAllStages()
                .anyMatch(this::hasFixableWorkspacePath);
    }

    private Boolean hasFixableWorkspacePath(@Nonnull Stage stage) {
        return stage
                .getWorkspace()
                .map(path -> Path.of(path).isAbsolute() || WRONG_STATIC_PATH_LITERAL_USED.equals(path))
                .orElse(Boolean.TRUE);
    }

    @Nonnull
    @CheckReturnValue
    public FixWorkspacePaths tryFixProjectsWithFixableWorkspacePaths(
            @Nonnull Orchestrator orchestrator) {
        Objects.requireNonNull(this.fixablePath);

        this.fixedPaths = new HashMap<>();
        this.fixFailed  = new ArrayList<>(this.fixablePath.size());

        for (var project : this.fixablePath) {
            var result = orchestrator.updatePipeline(project, pipeline -> {
                var stages   = pipeline.getAllStages().collect(Collectors.toUnmodifiableList());
                var response = new TreeMap<String, String>();

                for (var i = 0; i < stages.size(); ++i) {
                    var stage       = stages.get(i);
                    var stageNumber = i + 1;

                    if (hasFixableWorkspacePath(stage)) {
                        var path = WorkspaceCreator.getWorkspacePathOf(
                                pipeline.getProjectId(),
                                stageNumber,
                                stage.getDefinition()
                        ).toString();
                        stage.setWorkspace(path);
                        response.put(stage.getId(), path);
                    }
                }
                return response;
            });

            if (result.isEmpty()) {
                this.fixFailed.add(project);
            } else {
                this.fixedPaths.put(project, result.get());
            }
        }

        return this;
    }

    public void printResults(@Nonnull PrintStream ps) {
        Objects.requireNonNull(this.fixablePath);
        Objects.requireNonNull(this.loadFailed);
        Objects.requireNonNull(this.fixFailed);
        Objects.requireNonNull(this.fixedPaths);

        ps.println();
        ps.println(" Path fixing results");
        ps.println();

        if (this.fixablePath.isEmpty()) {
            ps.println("   Did not find any paths to fix");
        } else {
            ps.println("   Found the following project(s) with paths needing a fix");
            for (var fixable : this.fixablePath) {
                ps.println("     - " + fixable.getId());
            }
        }

        if (!this.loadFailed.isEmpty()) {
            ps.println();
            ps.println("   Failed to load the following project(s) to check for defect workspace paths");
            for (var failed : this.loadFailed) {
                ps.println("     - " + failed);
            }
        }

        if (!this.fixFailed.isEmpty()) {
            ps.println();
            ps.println("   Failed to fix paths for the following successfully loaded project(s)");
            for (var failed : this.fixFailed) {
                ps.println("     - " + failed.getId());
            }
        }


        if (!this.fixedPaths.isEmpty()) {
            ps.println();
            ps.println("   Fix paths for the following projects and stages");
            for (var fixed : this.fixedPaths.entrySet()) {
                ps.println("     - " + fixed.getKey().getId());
                for (var fixedStage : fixed.getValue().entrySet()) {
                    ps.println("       " + fixedStage.getKey());
                    ps.println("       => " + fixedStage.getValue());
                }
            }
        }
    }
}
