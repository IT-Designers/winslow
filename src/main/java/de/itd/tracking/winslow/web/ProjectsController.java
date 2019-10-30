package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.Image;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.EnqueuedStage;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.project.Project;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
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
    public Optional<Project> createProject(
            User user,
            @RequestParam("name") String name,
            @RequestParam("pipeline") String pipelineId) {
        return winslow.getPipelineRepository().getPipeline(pipelineId).unsafe().flatMap(pipelineDefinition -> winslow
                .getProjectRepository()
                .createProject(user, pipelineDefinition, project -> project.setName(name))
                .filter(project -> {
                    try {
                        winslow.getOrchestrator().createPipeline(project);
                        return true;
                    } catch (OrchestratorException e) {
                        LOG.log(Level.WARNING, "Failed to create pipeline for project", e);
                        if (!winslow.getProjectRepository().deleteProject(project.getId())) {
                            LOG.severe(
                                    "Failed to delete project for which no pipeline could be created, this leads to inconsistency!");
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
                        .getPipeline(project)
                        .stream()
                        .flatMap(pipeline -> Stream.concat(
                                pipeline.getCompletedStages(),
                                pipeline.getRunningStage().stream()
                        ))
                        .map(HistoryEntry::new));
    }

    @GetMapping("/projects/{projectId}/enqueued")
    public Stream<StageDefinition> getEnqueued(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .stream()
                        .flatMap(Pipeline::getEnqueuedStages)
                        .map(EnqueuedStage::getDefinition) // TODO does not expose Action information
                );
    }

    @DeleteMapping("/projects/{projectId}/enqueued/{index}/{controlSize}")
    public Optional<Boolean> deleteEnqueued(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("index") int index,
            @PathVariable("controlSize") int controlSize
    ) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .updatePipelineOmitExceptions(project, pipeline -> {
                            if (pipeline.getEnqueuedStages().count() == controlSize) {
                                return pipeline.removeEnqueuedStage(index).isPresent();
                            } else {
                                return Boolean.FALSE;
                            }
                        })
                );
    }

    @PostMapping("/projects/{projectId}/name")
    public void setProjectName(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("name") String name) throws LockException, IOException {
        var canAccess = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> canUserAccessProject(user, p));

        // do not try to lock expensively if the
        // user is not allowed to access the project anyway
        if (canAccess.isEmpty()) {
            return;
        }

        var exclusive = winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive();

        if (exclusive.isPresent()) {
            try (var project = exclusive.get()) {
                var update = project
                        .get()
                        .filter(p -> canUserAccessProject(user, p))
                        .map(p -> {
                            p.setName(name);
                            return p;
                        });

                if (update.isPresent()) {
                    project.update(update.get());
                }
            }
        }
    }

    @PostMapping("/projects/{projectId}/tags")
    public void setProjectName(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("tags") String[] tags) throws LockException, IOException {
        var canAccess = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> canUserAccessProject(user, p));

        // do not try to lock expensively if the
        // user is not allowed to access the project anyway
        if (canAccess.isEmpty()) {
            return;
        }

        var exclusive = winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive();

        if (exclusive.isPresent()) {
            try (var project = exclusive.get()) {
                var update = project
                        .get()
                        .filter(p -> canUserAccessProject(user, p))
                        .map(p -> {
                            p.setTags(tags);
                            return p;
                        });

                if (update.isPresent()) {
                    project.update(update.get());
                }
            }
        }
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
                        .getPipeline(project)
                        .flatMap(this::getPipelineState));
    }

    private Optional<Stage.State> getPipelineState(Pipeline pipeline) {
        return pipeline
                .getRunningStage()
                .map(Stage::getState)
                .or(
                        () -> {
                            if (!pipeline.isPauseRequested() && pipeline.hasEnqueuedStages()) {
                                return Optional.of(Stage.State.Running);
                            } else {
                                return Optional.empty();
                            }
                        }
                )
                .or(() -> pipeline.getMostRecentStage().map(Stage::getState).map(state -> {
                    switch (state) {
                        case Running:
                        case Failed:
                            return state;
                        default:
                            if (pipeline.isPauseRequested()) {
                                return Stage.State.Paused;
                            } else {
                                return state;
                            }
                    }
                }));
    }

    @GetMapping("/projects/states")
    public Stream<StateInfo> getProjectComplexStates(User user, @RequestParam("projectIds") String[] projectIds) {
        return Stream
                .of(projectIds)
                .map(winslow.getProjectRepository()::getProject)
                .map(BaseRepository.Handle::unsafe)
                .map(p -> p
                        .filter(project -> canUserAccessProject(user, project))
                        .flatMap(project -> winslow
                                .getOrchestrator()
                                .getPipeline(project))
                        .map(pipeline -> new StateInfo(
                                getPipelineState(pipeline).orElse(null),
                                pipeline
                                        .getPauseReason()
                                        .map(Pipeline.PauseReason::toString)
                                        .orElse(null),
                                winslow.getOrchestrator().getProgressHint(p.get()).orElse(null),
                                pipeline.hasEnqueuedStages()
                        ))
                        .orElse(null));
    }

    private Pipeline.Strategy getPipelineStrategy(@Nullable @RequestParam(value = "strategy", required = false) String strategy) {
        return Optional
                .ofNullable(strategy)
                .filter(str -> "once".equals(str.toLowerCase()))
                .map(str -> Pipeline.Strategy.MoveForwardOnce)
                .orElse(Pipeline.Strategy.MoveForwardUntilEnd);
    }

    @PostMapping("projects/{projectId}/paused/{paused}")
    public boolean setProjectNextStage(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("paused") boolean paused,
            @RequestParam(value = "strategy", required = false) @Nullable String strategy) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {
                    if (paused) {
                        pipeline.requestPause();
                    } else {
                        pipeline.setStrategy(getPipelineStrategy(strategy));
                        pipeline.resume(Pipeline.ResumeNotification.Confirmation);
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
                        .getPipeline(project)
                        .map(Pipeline::isPauseRequested))
                .orElse(false);
    }

    @GetMapping("projects/{projectId}/logs/latest")
    public Stream<LogEntryInfo> getProjectStageLogsLatest(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "skipLines", defaultValue = "0") long skipLines,
            @RequestParam(value = "expectingStageId", defaultValue = "0") String stageId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .flatMap(Pipeline::getMostRecentStage)
                        .stream()
                        .flatMap(stage -> {
                            var skip = skipLines;
                            if (stageId != null && !stageId.equals(stage.getId())) {
                                skip = 0;
                            }
                            return winslow
                                    .getOrchestrator()
                                    .getLogs(project, stage.getId())
                                    .map(entry -> new LogEntryInfo(stage.getId(), entry))
                                    .skip(skip);
                        }));
    }

    @GetMapping("projects/{projectId}/logs/{stageId}")
    public Stream<LogEntry> getProjectStgetProjectStageLogs(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId) {
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
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project))
                .flatMap(Pipeline::getPauseReason);
    }

    @GetMapping("projects/{projectId}/{stageIndex}/environment")
    public Map<String, String> getLatestEnvironment(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageIndex") int stageIndex) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .map(pipeline -> {
                            Map<String, String> map = new TreeMap<>();
                            project
                                    .getPipelineDefinition()
                                    .getStageDefinitions()
                                    .stream()
                                    .skip(stageIndex)
                                    .findFirst()
                                    .ifPresent(stageDef -> map.putAll(stageDef.getEnvironment()));
                            pipeline
                                    .getMostRecentStage()
                                    .ifPresent(stage -> map.putAll(stage.getEnv()));
                            return map;
                        })
                )
                .orElseGet(Collections::emptyMap);
    }

    @GetMapping("projects/{projectId}/{stageIndex}/required-user-input")
    public Stream<String> getLatestRequiredUserInput(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageIndex") int stageIndex) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> Stream.concat(
                        project
                                .getPipelineDefinition()
                                .getUserInput()
                                .stream()
                                .flatMap(u -> u.getValueFor().stream()),
                        project
                                .getPipelineDefinition()
                                .getStageDefinitions()
                                .stream()
                                .skip(stageIndex)
                                .findFirst()
                                .flatMap(StageDefinition::getUserInput)
                                .stream()
                                .flatMap(u -> u.getValueFor().stream())
                ));
    }

    @PostMapping("projects/{projectId}/pipeline/{pipelineId}")
    public Boolean changePipeline(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("pipelineId") String pipelineId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive()
                .map(projectContainer -> {
                    try (projectContainer) {
                        try {
                            var updatedProject = projectContainer
                                    .get()
                                    .filter(project -> canUserAccessProject(user, project))
                                    .flatMap(project -> winslow
                                            .getPipelineRepository()
                                            .getPipeline(pipelineId)
                                            .unsafe()
                                            .map(pipeline -> {
                                                project.setPipelineDefinition(pipeline);
                                                return project;
                                            }));

                            if (updatedProject.isPresent()) {
                                projectContainer.update(updatedProject.get());
                                return Boolean.TRUE;
                            }
                        } catch (LockException | IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }

                }).orElse(Boolean.FALSE);
    }

    @PutMapping("projects/{projectId}/enqueued")
    public void enqueueStage(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("env") Map<String, String> env,
            @RequestParam("stageIndex") int index,
            @RequestParam(value = "imageName", required = false) @Nullable String imageName,
            @RequestParam(value = "imageArgs", required = false) @Nullable String[] imageArgs) {
        winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .ifPresent(project -> winslow.getOrchestrator().updatePipelineOmitExceptions(project, pipeline -> {

                    // not cloning it is fine, because opened in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    var stageDef = getStageDefinitionNoClone(project, index);

                    stageDef = stageDef.map(def -> pipeline
                            .getAllStages()
                            .filter(stage -> stage.getDefinition().getName().equals(def.getName()))
                            .map(Stage::getDefinition)
                            .reduce((first, second) -> second)
                            .orElse(def)
                    );


                    if (stageDef.isPresent()) {
                        maybeUpdateImageInfo(imageName, imageArgs, stageDef.get());
                        pipeline.enqueueStage(createStageDefinition(env, stageDef.get()));
                    }

                    return pipeline;
                }));
    }

    @PutMapping("projects/configuration")
    public void enqueueConfigure(
            User user, @RequestParam("projectIds") String[] projectIds,
            @RequestParam("env") Map<String, String> env,
            @RequestParam("pipelineId") String pipelineId,
            @RequestParam("stageIndex") int stageIndex,
            @RequestParam(value = "image.name", required = false) @Nullable String imageName,
            @RequestParam(value = "image.args", required = false) @Nullable String[] imageArgs) {


        LOG.info("To configure: " + Arrays.toString(projectIds));
        LOG.info("   » env        " + env);
        LOG.info("   » pipelineId " + pipelineId);
        LOG.info("   » stageIndex " + stageIndex);
        LOG.info("   » image.name " + imageName);
        LOG.info("   » image.args " + Arrays.toString(imageArgs));

        winslow.getPipelineRepository().getPipeline(pipelineId).unsafe().ifPresent(pipelineDefinition -> {
            var stages   = pipelineDefinition.getStageDefinitions();
            var stageDef = stages.stream().skip(stageIndex).findFirst();

            LOG.info("     » pipeline name " + pipelineDefinition.getName());
            LOG.info("     » stage name    " + stageDef.map(StageDefinition::getName).orElse(null));

            if (stageDef.isPresent()) {
                maybeUpdateImageInfo(imageName, imageArgs, stageDef.get());
                var definition = createStageDefinition(env, stageDef.get());

                Stream
                        .of(projectIds)
                        .flatMap(id -> winslow
                                .getProjectRepository()
                                .getProject(id)
                                .unsafe()
                                .filter(project -> canUserAccessProject(user, project))
                                .stream()
                        )
                        .forEach(project -> winslow
                                .getOrchestrator()
                                .updatePipelineOmitExceptions(project, pipeline -> {
                                    pipeline.enqueueStage(definition, Action.Configure);
                                    return null;
                                }));
            }
        });
    }

    private StageDefinition createStageDefinition(@Nonnull Map<String, String> env, @Nonnull StageDefinition stageDef) {
        return new StageDefinition(
                stageDef.getName(),
                stageDef.getDescription().orElse(null),
                stageDef.getImage().orElse(null),
                stageDef.getRequirements().orElse(null),
                stageDef.getUserInput().orElse(null),
                env,
                stageDef.getHighlight().orElse(null)
        );
    }

    private void maybeUpdateImageInfo(
            @Nullable String imageName,
            @Nullable String[] imageArgs,
            @Nonnull StageDefinition stageDef) {
        if ((imageName != null || imageArgs != null)) {
            stageDef.getImage().ifPresent(image -> {
                if (imageName != null) {
                    image.setName(imageName);
                }
                if (imageArgs != null) {
                    image.setArgs(imageArgs);
                }
            });
        }
    }

    @Nonnull
    private static Optional<StageDefinition> getStageDefinitionNoClone(@Nonnull Project project, int index) {
        if (project.getPipelineDefinition().getStageDefinitions().size() > index) {
            return Optional.of(project.getPipelineDefinition().getStageDefinitions().get(index));
        }
        return Optional.empty();
    }

    @PutMapping("projects/{projectId}/kill")
    public void killCurrentStage(User user, @PathVariable("projectId") String projectId) throws LockException {
        var stage = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                )
                .flatMap(Pipeline::getRunningStage);

        if (stage.isPresent()) {
            winslow.getOrchestrator().kill(stage.get());
        }
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
        @Nonnull public final  String              stageId;
        @Nonnull public final  Date                startTime;
        @Nullable public final Date                finishTime;
        @Nonnull public final  Stage.State         state;
        @Nonnull public final  String              stageName;
        @Nonnull public final  String              workspace;
        @Nullable public final ImageInfo           imageInfo;
        @Nonnull public final  Map<String, String> env;
        @Nonnull public final  Map<String, String> envInternal;

        public HistoryEntry(Stage stage) {
            this.stageId     = stage.getId();
            this.startTime   = stage.getStartTime();
            this.finishTime  = stage.getFinishTime();
            this.state       = stage.getState();
            this.stageName   = stage.getDefinition().getName();
            this.workspace   = stage.getWorkspace();
            this.imageInfo   = stage.getDefinition().getImage().map(ImageInfo::new).orElse(null);
            this.env         = new TreeMap<>(stage.getEnv());
            this.envInternal = new TreeMap<>(stage.getEnvInternal());
        }
    }

    static class StateInfo {
        @Nullable public final Stage.State state;
        @Nullable public final String      pauseReason;
        @Nullable public final Integer     stageProgress;
        public final           boolean     hasEnqueuedStages;


        StateInfo(
                @Nullable Stage.State state,
                @Nullable String pauseReason,
                @Nullable Integer stageProgress,
                boolean hasEnqueuedStages) {
            this.state             = state;
            this.pauseReason       = pauseReason;
            this.stageProgress     = stageProgress;
            this.hasEnqueuedStages = hasEnqueuedStages;
        }
    }

    static class ImageInfo {
        @Nullable public final String   name;
        @Nullable public final String[] args;

        public ImageInfo(@Nonnull Image image) {
            this.name = image.getName();
            this.args = image.getArgs();
        }
    }

    static class LogEntryInfo extends LogEntry {

        public final String stageId;

        public LogEntryInfo(String stageId, LogEntry entry) {
            super(entry.getTime(), entry.getSource(), entry.isError(), entry.getMessage());
            this.stageId = stageId;
        }
    }
}
