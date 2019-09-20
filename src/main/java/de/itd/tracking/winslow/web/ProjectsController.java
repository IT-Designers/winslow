package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.config.UserInput;
import de.itd.tracking.winslow.project.Project;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
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
                .filter(project -> canUserAccessProject(user, project))
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
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {
                    if (paused) {
                        pipeline.requestPause();
                    } else {
                        pipeline.resume();
                    }
                    return Boolean.TRUE;
                }))
                .orElse(Boolean.FALSE);
    }

    @GetMapping("projects/{projectId}/paused")
    public boolean setProjectNextStage(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipelineOmitExceptions(project)
                        .map(Pipeline::isPauseRequested))
                .orElse(false);
    }

    @GetMapping("projects/{projectId}/logs/latest")
    public Stream<LogEntry> getProjectStageLogsLatest(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipelineOmitExceptions(project)
                        .flatMap(Pipeline::getMostRecentStage)
                        .stream()
                        .flatMap(stage -> winslow.getOrchestrator().getLogs(project, stage.getId())));
    }

    @GetMapping("projects/{projectId}/logs/{stageId}")
    public Stream<LogEntry> getProjectStageLogs(User user, @PathVariable("projectId") String projectId, @PathVariable("stageId") String stageId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow.getOrchestrator().getLogs(project, stageId));
    }

    @GetMapping("projects/{projectId}/pause-reason")
    public Optional<Pipeline.PauseReason> getPauseReason(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().getPipelineOmitExceptions(project))
                .flatMap(Pipeline::getPauseReason);
    }

    @GetMapping("projects/{projectId}/{stageIndex}/environment")
    public Map<String, String> getLatestEnvironment(User user, @PathVariable("projectId") String projectId, @PathVariable("stageIndex") int stageIndex) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().getPipelineOmitExceptions(project))
                .map(pipeline -> {
                    Map<String, String> map = new HashMap<>();
                    pipeline
                            .getDefinition()
                            .getStageDefinitions()
                            .stream()
                            .skip(stageIndex)
                            .map(StageDefinition::getEnvironment)
                            .forEach(map::putAll);
                    pipeline.getEnvironment().putAll(map);
                    return map;
                })
                .orElseGet(Collections::emptyMap);
    }

    @GetMapping("projects/{projectId}/{stageIndex}/required-user-input")
    public Stream<String> getLatestRequiredUserInput(User user, @PathVariable("projectId") String projectId, @PathVariable("stageIndex") int stageIndex) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().getPipelineOmitExceptions(project))
                .stream()
                .flatMap(pipeline -> Stream.concat(pipeline
                        .getDefinition()
                        .getUserInput()
                        .stream()
                        .flatMap(u -> u.getValueFor().stream()), pipeline
                        .getDefinition()
                        .getStageDefinitions()
                        .stream()
                        .skip(stageIndex)
                        .flatMap(s -> s.getUserInput().stream())
                        .flatMap(u -> u.getValueFor().stream())));
    }

    @PostMapping("projects/{projectId}/resume/{stageIndex}")
    public void resumePipeline(User user, @PathVariable("projectId") String projectId,
            @RequestParam("env") Map<String, String> env,
            @PathVariable("stageIndex") int index,
            @RequestParam(value = "strategy", required = false) @Nullable String strategy) {
        winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .ifPresent(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {
                    pipeline.setNextStageIndex(index);
                    pipeline.setStrategy(Optional
                            .ofNullable(strategy)
                            .filter(str -> "once".equals(str.toLowerCase()))
                            .map(str -> Pipeline.PipelineStrategy.MoveForwardOnce)
                            .orElse(Pipeline.PipelineStrategy.MoveForwardUntilEnd));
                    pipeline.getEnvironment().putAll(env);
                    pipeline.resume(Pipeline.ResumeNotification.Confirmation);
                    return null;
                }));
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
        public final String      stageId;
        public final Date        startTime;
        public final Date        finishTime;
        public final Stage.State state;
        public final String      stageName;
        public final String      workspace;

        public HistoryEntry(Stage stage) {
            this.stageId    = stage.getId();
            this.startTime  = stage.getStartTime();
            this.finishTime = stage.getFinishTime();
            this.state      = stage.getState();
            this.stageName  = stage.getDefinition().getName();
            this.workspace  = stage.getWorkspace();
        }
    }
}
