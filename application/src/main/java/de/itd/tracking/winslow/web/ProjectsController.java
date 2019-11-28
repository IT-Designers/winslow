package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.*;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.*;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.pipeline.*;
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
    public Stream<ProjectInfo> listProjects(User user) {
        return winslow
                .getProjectRepository()
                .getProjects()
                .flatMap(handle -> handle.unsafe().stream())
                .filter(project -> canUserAccessProject(user, project))
                .map(ProjectInfo::new);
    }

    @PostMapping("/projects")
    public Optional<ProjectInfo> createProject(
            User user,
            @RequestParam("name") String name,
            @RequestParam("pipeline") String pipelineId,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipelineId)
                .unsafe()
                .flatMap(pipelineDefinition -> winslow
                        .getProjectRepository()
                        .createProject(user, pipelineDefinition, project -> {
                            project.setName(name);
                            if (tags != null && tags.size() > 0) {
                                project.setTags(tags.toArray(new String[0]));
                            }
                        })
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
                        }))
                .map(ProjectInfo::new);
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
    public Stream<HistoryEntry> getEnqueued(User user, @PathVariable("projectId") String projectId) {
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
                )
                .map(HistoryEntry::new);
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

    @GetMapping("/projects/{projectId}/pipeline-definition")
    public Optional<PipelinesController.PipelineInfo> getProjectPipelineDefinition(
            User user,
            @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .map(Project::getPipelineDefinition)
                .map(definition -> new PipelinesController.PipelineInfo(projectId, definition));
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
                                        .getMostRecentStage()
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

                            return Stream.concat(
                                    winslow
                                            .getOrchestrator()
                                            .getLogs(project, stage.getId())  // do not stream in parallel!
                                            .skip(skip)
                                            .sequential()
                                            .map(entry -> new LogEntryInfo(
                                                    line.incrementAndGet(),
                                                    stage.getId(),
                                                    entry
                                            )),
                                    Stream
                                            .of(0L)
                                            .flatMap(dummy -> {
                                                if (line.get() == 0L) {
                                                    return Stream.of(new LogEntryInfo(
                                                            1L,
                                                            stage.getId() + "_temp",
                                                            new LogEntry(
                                                                    System.currentTimeMillis(),
                                                                    LogEntry.Source.MANAGEMENT_EVENT,
                                                                    false,
                                                                    "Loading..."
                                                            )
                                                    ));
                                                } else {
                                                    return Stream.empty();
                                                }
                                            })
                            );
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

    @GetMapping("projects/{projectId}/deletion-policy")
    public DeletionPolicy getDeletionPolicy(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project))
                .flatMap(Pipeline::getDeletionPolicy)
                .orElseGet(Orchestrator::defaultDeletionPolicy);
    }

    @PostMapping("projects/{projectId}/deletion-policy/number-of-workspaces-of-succeeded-stages-to-keep")
    public DeletionPolicy setDeletionPolicyNumberOfWorkspacesOfSucceededStagesToKeep(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "value", required = false) Integer value) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    var policy = pipeline.getDeletionPolicy().orElseGet(Orchestrator::defaultDeletionPolicy);
                    policy.setNumberOfWorkspacesOfSucceededStagesToKeep(value != null && value > 0 ? value : null);
                    pipeline.setDeletionPolicy(policy);
                    return policy;
                }))
                .orElseThrow();
    }

    @PostMapping("projects/{projectId}/deletion-policy/keep-workspace-of-failed-stage")
    public DeletionPolicy setDeletionPolicyKeepWorkspaceOfFailedStage(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "value", required = false) Boolean value) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    var policy = pipeline.getDeletionPolicy().orElseGet(Orchestrator::defaultDeletionPolicy);
                    policy.setKeepWorkspaceOfFailedStage(value != null ? value : true);
                    pipeline.setDeletionPolicy(policy);
                    return policy;
                }))
                .orElseThrow();
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
                            var resolver = new EnvVariableResolver()
                                    .withExecutionHistory(pipeline::getAllStages)
                                    .withEnqueuedStages(pipeline::getEnqueuedStages)
                                    .withInPipelineDefinitionDefinedVariables(
                                            project
                                                    .getPipelineDefinition()
                                                    .getEnvironment());


                            try {
                                resolver = resolver.withGlobalVariables(
                                        winslow
                                                .getSettingsRepository()
                                                .getGlobalEnvironmentVariables()
                                );
                            } catch (IOException e) {
                                LOG.log(Level.WARNING, "Failed to load system environment variables", e);
                            }

                            var stageDef = project
                                    .getPipelineDefinition()
                                    .getStages()
                                    .stream()
                                    .skip(stageIndex)
                                    .findFirst();

                            if (stageDef.isPresent()) {
                                resolver = resolver
                                        .withStageName(stageDef.get().getName())
                                        .withInStageDefinitionDefinedVariables(stageDef.get().getEnvironment());
                            }

                            return resolver.resolve();
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

    @PutMapping("projects/{projectId}/enqueued")
    public void enqueueStageToExecute(
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
                    return getStageDefinitionNoClone(project, index)
                            .map(stageDef -> {
                                enqueueExecutionStage(
                                        pipeline,
                                        stageDef,
                                        env,
                                        imageName,
                                        imageArgs
                                );
                                return Boolean.TRUE;
                            })
                            .orElse(Boolean.FALSE);
                }))
                .filter(v -> v)
                .orElseThrow();
    }

    @PutMapping("projects/{projectId}/enqueued-on-others")
    public Stream<Boolean> enqueueStageOnOthersToConfigure(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("stageIndex") int index,
            @RequestParam("projectIds") String[] projectIds,
            @RequestParam("env") Map<String, String> env,
            @RequestParam(value = "imageName", required = false) @Nullable String imageName,
            @RequestParam(value = "imageArgs", required = false) @Nullable String[] imageArgs
    ) {
        var stageDefinitionBase = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project).map(pipeline -> {
                    // not cloning it is fine, because opened in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    return getStageDefinitionNoClone(project, index);
                }))
                .orElseThrow()
                .orElseThrow();

        return Stream
                .of(projectIds)
                .map(id -> winslow.getProjectRepository().getProject(id).unsafe())
                .map(maybeProject -> maybeProject
                        .filter(project -> canUserAccessProject(user, project))
                        .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                            enqueueConfigureStage(
                                    pipeline,
                                    stageDefinitionBase,
                                    env,
                                    imageName,
                                    imageArgs
                            );
                            return Boolean.TRUE;
                        }))
                        .orElse(Boolean.FALSE));
    }

    private static void enqueueConfigureStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable String imageName,
            @Nullable String[] imageArgs) {
        enqueueStage(pipeline, base, env, imageName, imageArgs, Action.Configure);
    }

    private static void enqueueExecutionStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable String imageName,
            @Nullable String[] imageArgs) {
        enqueueStage(pipeline, base, env, imageName, imageArgs, Action.Execute);
    }

    private static void enqueueStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable String imageName,
            @Nullable String[] imageArgs,
            @Nonnull Action action) {

        var recentBase = Optional
                .of(base)
                .flatMap(def -> pipeline
                        .getAllStages()
                        .filter(stage -> stage.getDefinition().getName().equals(def.getName()))
                        .map(Stage::getDefinition)
                        .reduce((first, second) -> second)
                )
                .orElse(base); // none before, so take the given as origin

        var resultDefinition = createStageDefinition(
                recentBase,
                base.getRequirements().orElse(null),
                base.getRequires().orElse(null),
                env
        );

        /*
        // configurations can slip through even if paused
        if (!pipeline.hasEnqueuedStages() && action == Action.Configure) {
            if (pipeline.isPauseRequested()) {
                pipeline.setStrategy(Pipeline.Strategy.MoveForwardOnce);
            }
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        }
         */
        maybeUpdateImageInfo(imageName, imageArgs, resultDefinition);
        pipeline.enqueueStage(resultDefinition, action);
        resumeIfPausedByStageFailure(pipeline);
    }

    private static void resumeIfPausedByStageFailure(@Nonnull Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.StageFailure).equals(pipeline.getPauseReason())) {
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        }
    }

    private static StageDefinition createStageDefinition(
            @Nonnull StageDefinition template,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nonnull Map<String, String> env) {
        return new StageDefinitionBuilder()
                .withBase(template)
                .withRequirements(requirements)
                .withUserInput(requires)
                .withEnvironment(env)
                .build();
    }

    private static void maybeUpdateImageInfo(
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
                    try {
                        winslow.getOrchestrator().deletePipeline(project.get());
                    } catch (PipelineNotFoundException e) {
                        // this is fine
                    }
                    if (!container.deleteOmitExceptions()) {
                        LOG.log(Level.SEVERE, "Deleted Pipeline but failed to delete Project " + projectId);
                        return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Deleted corresponding Pipeline but failed to delete the Project. Please contact the system administrator!");
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

    static class ProjectInfo {
        public final @Nonnull String                           id;
        public final @Nonnull String                           owner;
        public final @Nonnull List<String>                     groups;
        public final @Nonnull List<String>                     tags;
        public final @Nonnull String                           name;
        public final @Nonnull PipelinesController.PipelineInfo pipelineDefinition;

        public ProjectInfo(@Nonnull Project project) {
            this.id                 = project.getId();
            this.owner              = project.getOwner();
            this.groups             = project.getGroups();
            this.tags               = project.getTags();
            this.name               = project.getName();
            this.pipelineDefinition = new PipelinesController.PipelineInfo(
                    project.getId(),
                    project.getPipelineDefinition()
            );
        }
    }

    static class HistoryEntry {
        public final @Nullable String              stageId;
        public final @Nullable Date                startTime;
        public final @Nullable Date                finishTime;
        public final @Nullable Stage.State         state;
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

        public HistoryEntry(EnqueuedStage enqueuedStage) {
            this.stageId     = null;
            this.startTime   = null;
            this.finishTime  = null;
            this.state       = null;
            this.action      = enqueuedStage.getAction();
            this.stageName   = enqueuedStage.getDefinition().getName();
            this.workspace   = null;
            this.imageInfo   = enqueuedStage.getDefinition().getImage().map(ImageInfo::new).orElse(null);
            this.env         = new TreeMap<>(enqueuedStage.getDefinition().getEnvironment());
            this.envPipeline = Collections.emptyMap();
            this.envSystem   = Collections.emptyMap();
            this.envInternal = Collections.emptyMap();
        }
    }

    static class StateInfo {
        @Nullable public final Stage.State state;
        @Nullable public final String      pauseReason;
        @Nullable public final String      mostRecentStage;
        @Nullable public final Integer     stageProgress;
        public final           boolean     hasEnqueuedStages;


        StateInfo(
                @Nullable Stage.State state,
                @Nullable String pauseReason,
                @Nullable String mostRecentStage,
                @Nullable Integer stageProgress,
                boolean hasEnqueuedStages) {
            this.state             = state;
            this.pauseReason       = pauseReason;
            this.mostRecentStage   = mostRecentStage;
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

    public static class EnvVariable {
        public final @Nonnull String key;
        public @Nullable      String value;
        public @Nullable      String valueInherited;

        public EnvVariable(@Nonnull String key, @Nullable String value) {
            this.key            = key;
            this.value          = value;
            this.valueInherited = value;
        }

        public EnvVariable(@Nonnull String key) {
            this.key            = key;
            this.value          = null;
            this.valueInherited = null;
        }

        public void pushValue(@Nullable String value) {
            if (this.value != null) {
                this.valueInherited = this.value;
            }
            this.value = value;
        }
    }
}
