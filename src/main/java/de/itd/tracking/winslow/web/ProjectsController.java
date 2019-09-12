package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.Pipeline;
import de.itd.tracking.winslow.Stage;
import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.project.Project;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@RestController
public class ProjectsController {

    private static final Logger LOG = Logger.getLogger(ProjectsController.class.getSimpleName());

    private final Winslow winslow;

    public ProjectsController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/projects")
    public Stream<Project> listProjects(User user) {
        return winslow
                .getProjectRepository()
                .getProjects()
                .flatMap(handle -> handle.unsafe().stream())
                .filter(project -> canUserAccessProject(user, project));
    }

    @PostMapping("/projects")
    public Optional<Project> createProject(User user, @RequestParam("name") String name, @RequestParam("pipeline") String pipelineId) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipelineId)
                .unsafe()
                .flatMap(pipelineDefinition -> winslow
                        .getProjectRepository()
                        .createProject(user, pipelineDefinition, project -> project.setName(name))
                        .filter(project -> {
                            try {
                                winslow.getOrchestrator().createPipeline(project);
                                return true;
                            } catch (OrchestratorException e) {
                                LOG.log(Level.WARNING, "Failed to create pipeline for project", e);
                                if (!winslow.getProjectRepository().deleteProject(project.getId())) {
                                    LOG.severe("Failed to delete project for which no pipeline could be created, this leads to inconsistency!");
                                }
                                return false;
                            }
                        }));
    }

    @GetMapping("/projects/{projectId}/history")
    public Stream<HistoryEntry> getProjectHistory(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipelineOmitExceptions(project)
                        .stream()
                        .flatMap(pipeline -> Stream.concat(pipeline.getCompletedStages(), pipeline
                                .getRunningStage()
                                .stream()))
                        .map(HistoryEntry::new));
    }

    @GetMapping("/projects/{projectId}/state")
    public Optional<Stage.State> getProjectState(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipelineOmitExceptions(project)
                        .flatMap(pipeline -> pipeline.getMostRecentStage().map(Stage::getState).map(state -> {
                            if (state != Stage.State.Running && pipeline.isPauseRequested()) {
                                return Stage.State.Paused;
                            } else {
                                return state;
                            }
                        })));
    }

    @PostMapping("projects/{projectId}/nextStage/{stageIndex}")
    public boolean setProjectNextStage(User user, @PathVariable("projectId") String projectId, @PathVariable("stageIndex") int index, @RequestParam(value = "strategy", required = false) @Nullable String strategy) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .flatMap(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {
                    pipeline.setNextStageIndex(index);
                    pipeline.setStrategy(Optional
                            .ofNullable(strategy)
                            .filter(str -> "once".equals(str.toLowerCase()))
                            .map(str -> Pipeline.PipelineStrategy.MoveForwardOnce)
                            .orElse(Pipeline.PipelineStrategy.MoveForwardUntilEnd));
                    pipeline.resume();
                    return true;
                }))
                .orElse(false);
    }

    @PostMapping("projects/{projectId}/paused/{paused}")
    public boolean setProjectNextStage(User user, @PathVariable("projectId") String projectId, @PathVariable("paused") boolean paused) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .flatMap(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {
                    if (paused) {
                        pipeline.requestPause();
                    } else {
                        pipeline.resume();
                    }
                    return true;
                }))
                .orElse(false);
    }

    @GetMapping("projects/{projectId}/paused")
    public boolean setProjectNextStage(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipelineOmitExceptions(project)
                        .map(Pipeline::isPauseRequested))
                .orElse(false);
    }

    private boolean canUserAccessProject(@Nonnull User user, @Nonnull Project project) {
        return project.getOwner().equals(user.getName()) || user.getGroups().anyMatch(g -> {
            for (String group : project.getGroups()) {
                if (group.equals(g)) {
                    return true;
                }
            }
            return false;
        });
    }

    static class HistoryEntry {
        public final Date        startTime;
        public final Date        finishTime;
        public final Stage.State state;
        public final String      stageName;
        public final String      workspace;

        public HistoryEntry(Stage stage) {
            this.startTime  = stage.getStartTime();
            this.finishTime = stage.getFinishTime();
            this.state      = stage.getState();
            this.stageName  = stage.getDefinition().getName();
            this.workspace  = stage.getWorkspace();
        }
    }
}
