package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.LogEntry;
import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.Image;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.EnqueuedStage;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.project.ProjectRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
                        .updatePipeline(project, pipeline -> {
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
                                pipeline
                                        .getRunningStage()
                                        .map(Stage::getDefinition)
                                        .map(StageDefinition::getName)
                                        .orElse(null),
                                pipeline.getRunningStage()
                                        .map(Stage::getId)
                                        .flatMap(winslow.getRunInfoRepository()::getProgressHint)
                                        .orElse(null),
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
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
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

                            var line = new AtomicLong(skip);

                            return winslow
                                    .getOrchestrator()
                                    .getLogs(project, stage.getId())  // do not stream in parallel!
                                    .skip(skip)
                                    .map(entry -> new LogEntryInfo(line.incrementAndGet(), stage.getId(), entry));
                        }));
    }

    @GetMapping("projects/{projectId}/logs/{stageId}")
    public Stream<LogEntryInfo> getProjectStageLogs(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId) {
        var line = new AtomicLong(0);
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow.getOrchestrator().getLogs(project, stageId))
                .map(entry -> new LogEntryInfo(line.incrementAndGet(), stageId, entry));
    }

    @GetMapping("projects/{projectId}/raw-logs/{stageId}")
    public ResponseEntity<InputStreamResource> getProjectRawLogs(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> {
                    try {
                        return Optional.of(winslow
                                                   .getLogRepository()
                                                   .getRawInputStreamNonExclusive(
                                                           projectId,
                                                           stageId
                                                   ));
                    } catch (FileNotFoundException e) {
                        return Optional.empty();
                    }
                })
                .map(e -> ResponseEntity
                        .ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(new InputStreamResource(e))
                )
                .orElse(null);
    }

    @GetMapping("projects/{projectId}/pipeline-definition-raw")
    public Optional<String> getProjectRawDefinition(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> {
                    try (var baos = new ByteArrayOutputStream()) {
                        ProjectRepository.defaultWriter().store(baos, project.getPipelineDefinition());
                        return Optional.of(baos.toString(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Failed to serialize PipelineDefinition only", e);
                        return Optional.empty();
                    }
                });
    }

    @PostMapping("projects/{projectId}/pipeline-definition-raw")
    public ResponseEntity<String> getProjectRawDefinition(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("raw") String raw) throws IOException, LockException {

        var definition = (PipelineDefinition) null;

        try {
            definition = PipelinesController.tryParsePipelineDef(raw);
        } catch (PipelinesController.ParseErrorException e) {
            return PipelinesController.toJsonResponseEntity(e.getParseError());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to deserialize PipelineDefinition", e);
            return ResponseEntity.ok(e.getMessage());
        }

        var containerOptional = winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive();

        if (containerOptional.isPresent()) {
            var container = containerOptional.get();
            try (container) {
                var maybeProject = container.get();
                if (!maybeProject.map(p -> canUserAccessProject(user, p)).orElse(Boolean.FALSE)) {
                    return ResponseEntity.notFound().build();
                } else {
                    var project = maybeProject.get();
                    project.setPipelineDefinition(definition);
                    container.update(project);
                }
            }
        }
        return ResponseEntity.ok(null);
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
    public Map<String, EnvVariable> getLatestEnvironment(
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
                            Map<String, EnvVariable> map = new TreeMap<>();
                            try {
                                winslow
                                        .getSettingsRepository()
                                        .getGlobalEnvironmentVariables()
                                        .forEach((key, value) -> {
                                            map.put(key, new EnvVariable(key).inherited(value));
                                        });
                            } catch (IOException e) {
                                LOG.log(Level.WARNING, "Failed to load system environment variables", e);
                            }
                            project.getPipelineDefinition().getEnvironment().forEach((key, value) -> {
                                map.computeIfAbsent(key, k -> new EnvVariable(key)).inherited(value);
                            });
                            project
                                    .getPipelineDefinition()
                                    .getStages()
                                    .stream()
                                    .skip(stageIndex)
                                    .findFirst()
                                    .ifPresent(stageDef -> {
                                        stageDef.getEnvironment().forEach((key, value) -> {
                                            map.computeIfAbsent(
                                                    key,
                                                    k -> new EnvVariable(key, value)
                                            ).updateValue(value);
                                        });
                                    });
                            pipeline
                                    .getMostRecentStage()
                                    .ifPresent(stage -> {
                                        stage.getEnv().forEach((key, value) -> {
                                            map.computeIfAbsent(
                                                    key,
                                                    k -> new EnvVariable(key, value)
                                            ).updateValue(value);
                                        });
                                    });
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
                                .getRequires()
                                .stream()
                                .flatMap(u -> u.getEnvironment().stream()),
                        project
                                .getPipelineDefinition()
                                .getStages()
                                .stream()
                                .skip(stageIndex)
                                .findFirst()
                                .flatMap(StageDefinition::getRequires)
                                .stream()
                                .flatMap(u -> u.getEnvironment().stream())
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

    @DeleteMapping("projects/{projectId}")
    public ResponseEntity<String> delete(User user, @PathVariable("projectId") String projectId) {
        var project = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> canUserAccessProject(user, p));

        if (project.isPresent()) {
            var exclusive = winslow.getProjectRepository().getProject(projectId).exclusive();

            if (exclusive.isPresent()) {
                try (var container = exclusive.get()) {
                    if (winslow.getOrchestrator().deletePipeline(project.get())) {
                        if (!container.deleteOmitExceptions()) {
                            LOG.log(Level.SEVERE, "Deleted Pipeline but failed to delete Project " + projectId);
                            return ResponseEntity
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Deleted corresponding Pipeline but failed to delete the Project. Please contact the system administrator!");
                        }
                    }
                } catch (OrchestratorException e) {
                    LOG.log(
                            Level.SEVERE,
                            "Failed to delete Pipeline for Project " + project.get().getName() + "/" + projectId,
                            e
                    );
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Dependency error: Failed to delete the Pipeline: " + e.getMessage());
                }
            }
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
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
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {

                    // not cloning it is fine, because opened in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    var stageFromPipeline = getStageDefinitionNoClone(project, index);
                    var stageDef          = stageFromPipeline;

                    stageDef = stageDef.map(def -> pipeline
                            .getAllStages()
                            .filter(stage -> stage.getDefinition().getName().equals(def.getName()))
                            .map(Stage::getDefinition)
                            .reduce((first, second) -> second)
                            .orElse(def)
                    );

                    if (stageDef.isPresent()) {
                        stageDef = Optional.of(new StageDefinition(
                                stageDef.get().getName(),
                                stageDef.get().getDescription().orElse(null),
                                stageDef.get().getImage().orElse(null),
                                stageFromPipeline.get().getRequirements().orElse(null),
                                stageFromPipeline.get().getRequires().orElse(null),
                                stageDef.get().getEnvironment(),
                                stageDef.get().getHighlight().orElse(null)
                        ));

                        if (!pipeline.hasEnqueuedStages()) {
                            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
                        }
                        maybeUpdateImageInfo(imageName, imageArgs, stageDef.get());
                        pipeline.enqueueStage(createStageDefinition(env, stageDef.get()));

                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }))
                .filter(v -> v)
                .orElseThrow();
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
            var stages   = pipelineDefinition.getStages();
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
                                .updatePipeline(project, pipeline -> {
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
                stageDef.getRequires().orElse(null),
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
        if (project.getPipelineDefinition().getStages().size() > index) {
            return Optional.of(project.getPipelineDefinition().getStages().get(index));
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
        return user.hasSuperPrivileges() || project.getOwner().equals(user.getName()) || user
                .getGroups()
                .anyMatch(g -> {
                    for (String group : project.getGroups()) {
                        if (group.equals(g)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    static class HistoryEntry {
        public final @Nonnull  String              stageId;
        public final @Nonnull  Date                startTime;
        public final @Nullable Date                finishTime;
        public final @Nonnull  Stage.State         state;
        public final @Nonnull  Action              action;
        public final @Nonnull  String              stageName;
        public final @Nullable String              workspace;
        public final @Nullable ImageInfo           imageInfo;
        public final @Nonnull  Map<String, String> env;
        public final @Nonnull  Map<String, String> envPipeline;
        public final @Nonnull  Map<String, String> envSystem;
        public final @Nonnull  Map<String, String> envInternal;

        public HistoryEntry(Stage stage) {
            this.stageId     = stage.getId();
            this.startTime   = stage.getStartTime();
            this.finishTime  = stage.getFinishTime().orElse(null);
            this.state       = stage.getState();
            this.action      = stage.getAction();
            this.stageName   = Optional.ofNullable(stage.getDefinition()).map(StageDefinition::getName).orElse(null);
            this.workspace   = stage.getWorkspace().orElse(null);
            this.imageInfo   = Optional.ofNullable(stage.getDefinition()).flatMap(StageDefinition::getImage).map(
                    ImageInfo::new).orElse(null);
            this.env         = new TreeMap<>(stage.getEnv());
            this.envPipeline = new TreeMap<>(stage.getEnvPipeline());
            this.envSystem   = new TreeMap<>(stage.getEnvSystem());
            this.envInternal = new TreeMap<>(stage.getEnvInternal());
        }
    }

    static class StateInfo {
        @Nullable public final Stage.State state;
        @Nullable public final String      pauseReason;
        @Nullable public final String      runningStage;
        @Nullable public final Integer     stageProgress;
        public final           boolean     hasEnqueuedStages;


        StateInfo(
                @Nullable Stage.State state,
                @Nullable String pauseReason,
                @Nullable String runningStage,
                @Nullable Integer stageProgress,
                boolean hasEnqueuedStages) {
            this.state             = state;
            this.pauseReason       = pauseReason;
            this.runningStage      = runningStage;
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

        public final long   line;
        public final String stageId;

        public LogEntryInfo(long line, String stageId, LogEntry entry) {
            super(entry.getTime(), entry.getSource(), entry.isError(), entry.getMessage());
            this.line    = line;
            this.stageId = stageId;
        }
    }

    static class EnvVariable {
        public final @Nonnull String key;
        public @Nullable      String value;
        public @Nullable      String valueInherited;

        EnvVariable(@Nonnull String key) {
            this(key, null);
        }

        EnvVariable(@Nonnull String key, @Nullable String value) {
            this.key            = key;
            this.value          = value;
            this.valueInherited = null;
        }

        EnvVariable inherited(@Nullable String value) {
            this.valueInherited = value;
            return this;
        }

        void updateValue(@Nullable String value) {
            if (this.value != null) {
                this.valueInherited = this.value;
            }
            this.value = value;
        }
    }
}
