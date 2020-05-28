package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.*;
import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.PipelineInfo;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.project.*;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.web.HistoryEntryConverter;
import de.itdesigners.winslow.web.PipelineInfoConverter;
import de.itdesigners.winslow.web.ProjectInfoConverter;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                .filter(project -> project.canBeAccessedBy(user))
                .map(ProjectInfoConverter::from);
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
                .map(ProjectInfoConverter::from);
    }

    @GetMapping("/projects/{projectId}/history")
    public Stream<HistoryEntry> getProjectHistory(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .stream()
                        .flatMap(pipeline -> Stream.concat(
                                pipeline.getCompletedStages(),
                                pipeline.getRunningStage().stream()
                        ))
                        .map(HistoryEntryConverter::from)
                );
    }

    @PostMapping("/projects/{projectId}/history/prune")
    public Stream<HistoryEntry> pruneProjectHistory(User user, @PathVariable("projectId") String projectId) throws IOException {
        var project = getProjectIfAllowedToAccess(user, projectId);
        if (project.isPresent()) {
            winslow.getOrchestrator().prunePipeline(project.get());
            return getProjectHistory(user, projectId);
        } else {
            return Stream.empty();
        }
    }

    @GetMapping("/projects/{projectId}/enqueued")
    public Stream<HistoryEntry> getEnqueued(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .stream()
                        .flatMap(Pipeline::getEnqueuedStages)
                )
                .map(HistoryEntryConverter::from);
    }

    @DeleteMapping("/projects/{projectId}/enqueued/{index}/{controlSize}")
    public Optional<Boolean> deleteEnqueued(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("index") int index,
            @PathVariable("controlSize") int controlSize
    ) {
        return getProjectIfAllowedToAccess(user, projectId)
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
    public ResponseEntity<String> setProjectName(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("name") String name) throws LockException, IOException {
        var canAccess = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> p.canBeManagedBy(user));

        // do not try to lock expensively if the
        // user is not allowed to access the project anyway
        if (canAccess.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var exclusive = winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive();

        if (exclusive.isPresent()) {
            try (var project = exclusive.get()) {
                var update = project
                        .get()
                        .filter(p -> p.canBeAccessedBy(user))
                        .map(p -> {
                            p.setName(name);
                            return p;
                        });

                if (update.isPresent()) {
                    project.update(update.get());
                    return ResponseEntity.ok().build();
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/projects/{projectId}/tags")
    public ResponseEntity<String[]> setProjectTags(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("tags") String[] tags) throws LockException, IOException {
        var canAccess = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> p.canBeManagedBy(user));

        // do not try to lock expensively if the
        // user is not allowed to access the project anyway
        if (canAccess.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var exclusive = winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive();

        if (exclusive.isPresent()) {
            try (var project = exclusive.get()) {
                var update = project
                        .get()
                        .filter(p -> p.canBeAccessedBy(user))
                        .map(p -> {
                            p.setTags(tags);
                            return p;
                        });

                if (update.isPresent()) {
                    project.update(update.get());
                    return ResponseEntity.ok(tags);
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/projects/{projectId}/pipeline-definition")
    public Optional<PipelineInfo> getProjectPipelineDefinition(
            User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .map(Project::getPipelineDefinition)
                .map(definition -> PipelineInfoConverter.from(projectId, definition));
    }

    @GetMapping("/projects/{projectId}/state")
    public Optional<State> getProjectState(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .flatMap(this::getPipelineState));
    }

    private Optional<State> getPipelineState(Pipeline pipeline) {
        return pipeline
                .getRunningStage()
                .map(Stage::getState)
                .or(
                        () -> {
                            if (!pipeline.isPauseRequested() && pipeline.hasEnqueuedStages()) {
                                return Optional.of(State.Running);
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
                                return State.Paused;
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
                        .filter(project -> project.canBeAccessedBy(user))
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
        return getProjectIfAllowedToAccess(user, projectId)
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
        return getProjectIfAllowedToAccess(user, projectId)
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
        return getProjectIfAllowedToAccess(user, projectId)
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
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow.getOrchestrator().getLogs(project, stageId))
                .map(entry -> new LogEntryInfo(line.incrementAndGet(), stageId, entry));
    }

    @GetMapping("projects/{projectId}/raw-logs/{stageId}")
    public ResponseEntity<InputStreamResource> getProjectRawLogs(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId) {
        return getProjectIfAllowedToAccess(user, projectId)
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
        return getProjectIfAllowedToAccess(user, projectId)
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
    public ResponseEntity<String> setProjectRawDefinition(
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
                var maybeProject = container.get().filter(p -> p.canBeManagedBy(user));
                if (maybeProject.isEmpty()) {
                    return ResponseEntity.notFound().build();
                } else {
                    var project = maybeProject.get();
                    project.setPipelineDefinition(definition);
                    container.update(project);
                    return ResponseEntity.ok().build();
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("projects/{projectId}/pause-reason")
    public Optional<Pipeline.PauseReason> getPauseReason(User user, @PathVariable("projectId") String projectId) {
        return getPipelineIfAllowedToAccess(user, projectId).flatMap(Pipeline::getPauseReason);
    }

    @GetMapping("projects/{projectId}/deletion-policy")
    public Optional<DeletionPolicy> getDeletionPolicy(User user, @PathVariable("projectId") String projectId) {
        return getPipelineIfAllowedToAccess(user, projectId).flatMap(Pipeline::getDeletionPolicy);
    }

    @GetMapping("projects/{projectId}/deletion-policy/default")
    public DeletionPolicy getDeletionPolicyDefault(User user, @PathVariable("projectId") String projectId) {
        return Orchestrator.defaultDeletionPolicy();
    }

    @DeleteMapping("projects/{projectId}/deletion-policy")
    public ResponseEntity<Boolean> resetDeletionPolicy(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeManagedBy(user))
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    pipeline.setDeletionPolicy(null);
                    return ResponseEntity.ok(Boolean.TRUE); // just _some_ value
                }))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("projects/{projectId}/deletion-policy")
    public ResponseEntity<DeletionPolicy> setDeletionPolicyNumberOfWorkspacesOfSucceededStagesToKeep(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("value") DeletionPolicy policy) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeManagedBy(user))
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    pipeline.setDeletionPolicy(policy);
                    return ResponseEntity.ok(policy); // just _some_ value
                }))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("projects/{projectId}/{stageIndex}/environment")
    public Map<String, EnvVariable> getLatestEnvironment(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageIndex") int stageIndex) {
        return getProjectIfAllowedToAccess(user, projectId)
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
                                                    .getEnvironment()
                                    );


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
        return getProjectIfAllowedToAccess(user, projectId)
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
    public ResponseEntity<Boolean> setPipelineDefinition(
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
                                    .filter(project -> project.canBeManagedBy(user))
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
                                return ResponseEntity.ok(true);
                            }
                        } catch (LockException | IOException e) {
                            e.printStackTrace();
                        }

                        return ResponseEntity.notFound().<Boolean>build();
                    }

                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("projects/{projectId}/enqueued")
    public void enqueueStageToExecute(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("env") Map<String, String> env,
            @RequestParam("stageIndex") int index,
            @RequestParam(value = "image", required = false) @Nullable ImageInfo image,
            @RequestParam(value = "requiredResources", required = false) @Nullable ResourceInfo requiredResources
    ) {
        getProjectIfAllowedToAccess(user, projectId)

                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {

                    // not cloning it is fine, because it was loaded in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    return getStageDefinitionNoClone(project, index)
                            .map(stageDef -> {
                                enqueueExecutionStage(
                                        pipeline,
                                        stageDef,
                                        env,
                                        image,
                                        requiredResources
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
            @RequestParam(value = "image", required = false) @Nullable ImageInfo image,
            @RequestParam(value = "requiredResources", required = false) @Nullable ResourceInfo requiredResources
    ) {
        var stageDefinitionBase = getProjectIfAllowedToAccess(user, projectId)
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
                        .filter(project -> project.canBeAccessedBy(user))
                        .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                            enqueueConfigureStage(
                                    pipeline,
                                    stageDefinitionBase,
                                    env,
                                    image,
                                    requiredResources
                            );
                            return Boolean.TRUE;
                        }))
                        .orElse(Boolean.FALSE));
    }

    @PutMapping("projects/{projectId}/action/{actionId}")
    public void enqueueAction(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("actionId") String actionId) {
        getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getPipelineRepository()
                        .getPipeline(actionId)
                        .unsafe()
                        .flatMap(pipelineDefinition -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {

                            for (var stage : pipelineDefinition.getStages()) {
                                pipeline.enqueueStage(
                                        new StageDefinitionBuilder()
                                                .withTemplateBase(stage)
                                                .withEnvironment(pipelineDefinition.getEnvironment())
                                                .withAdditionalEnvironment(stage.getEnvironment())
                                                .build(),
                                        Action.Execute
                                );
                            }

                            resumeIfPausedByStageFailure(pipeline);
                            return Boolean.TRUE;
                        })))
                .filter(v -> v)
                .orElseThrow();
    }

    private static void enqueueConfigureStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources) {
        enqueueStage(pipeline, base, env, image, requiredResources, Action.Configure);
    }

    private static void enqueueExecutionStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources) {
        enqueueStage(pipeline, base, env, image, requiredResources, Action.Execute);
    }

    private static void enqueueStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources,
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
                base,
                recentBase,
                updatedResourceRequirement(base.getRequirements().orElse(null), requiredResources),
                base.getRequires().map(UserInput::withoutConfirmation).orElse(null),
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
        maybeUpdateImageInfo(image, resultDefinition);
        pipeline.enqueueStage(resultDefinition, action);
        resumeIfPausedByStageFailure(pipeline);
        resumeIfWaitingForGoneStageConfiramtion(pipeline);
    }

    @Nullable
    private static Requirements updatedResourceRequirement(
            @Nullable Requirements requirements,
            @Nullable ResourceInfo update) {
        if (requirements == null && update == null) {
            return null;
        } else if (requirements != null && update == null) {
            // nothing to do
            return requirements;
        } else {
            return new Requirements(
                    update.cpus,
                    update.megabytesOfRam,
                    Optional.ofNullable(update.gpus)
                            .map(gpus -> new Requirements.Gpu(
                                    gpus,
                                    Optional
                                            .ofNullable(requirements)
                                            .map(Requirements::getGpu)
                                            .flatMap(g -> g.flatMap(Requirements.Gpu::getVendor))
                                            .orElse(null),
                                    Optional
                                            .ofNullable(requirements)
                                            .map(Requirements::getGpu)
                                            .flatMap(g -> g.map(Requirements.Gpu::getSupport))
                                            .orElse(null)
                            ))
                            .orElse(null)
            );
        }
    }

    private static void resumeIfWaitingForGoneStageConfiramtion(Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.ConfirmationRequired).equals(pipeline.getPauseReason())) {
            var noStageRequiresUserConfirmation = pipeline
                    .getEnqueuedStages()
                    .noneMatch(e -> e
                            .getDefinition()
                            .getRequires()
                            .map(UserInput::getConfirmation)
                            .orElse(UserInput.Confirmation.Never)
                            != UserInput.Confirmation.Never);
            if (noStageRequiresUserConfirmation) {
                pipeline.resume(Pipeline.ResumeNotification.Confirmation);
            }
        }
    }

    private static void resumeIfPausedByStageFailure(@Nonnull Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.StageFailure).equals(pipeline.getPauseReason())) {
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        }
    }

    private static StageDefinition createStageDefinition(
            @Nonnull StageDefinition templateBase,
            @Nullable StageDefinition recentBase,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nonnull Map<String, String> env) {
        return new StageDefinitionBuilder()
                .withTemplateBase(templateBase)
                .withRecentBase(recentBase)
                .withRequirements(requirements)
                .withUserInput(requires)
                .withEnvironment(env)
                .build();
    }

    private static void maybeUpdateImageInfo(
            @Nullable ImageInfo image,
            @Nonnull StageDefinition stageDef) {
        if (image != null) {
            stageDef.getImage().ifPresent(def -> {
                Optional.ofNullable(image.name).ifPresent(def::setName);
                Optional.ofNullable(image.args).ifPresent(def::setArgs);
                Optional.ofNullable(image.shmMegabytes).ifPresent(def::setShmSizeMegabytes);
            });
        }
    }

    @DeleteMapping("projects/{projectId}")
    public ResponseEntity<String> delete(User user, @PathVariable("projectId") String projectId) {
        var project = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> p.canBeManagedBy(user));

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

    @PutMapping("projects/{projectId}/stop")
    public void stopCurrentStage(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestParam(name = "pause", required = false, defaultValue = "true") boolean pause) throws LockException {
        var project = getProjectIfAllowedToAccess(user, projectId);
        var stage = project
                .flatMap(p -> winslow
                        .getOrchestrator()
                        .getPipeline(p)
                )
                .flatMap(Pipeline::getRunningStage);
        if (stage.isPresent()) {
            winslow.getOrchestrator().stop(stage.get());
            if (pause) {
                winslow.getOrchestrator().updatePipeline(project.get(), pipeline -> {
                    pipeline.requestPause();
                    return null;
                });
            }
        }
    }

    @PutMapping("projects/{projectId}/kill")
    public void killCurrentStage(User user, @PathVariable("projectId") String projectId) throws LockException {
        var stage = getCurrentlyRunningStageIfAllowedToAccess(user, projectId);
        if (stage.isPresent()) {
            winslow.getOrchestrator().kill(stage.get());
        }
    }

    @Nonnull
    private Optional<Stage> getCurrentlyRunningStageIfAllowedToAccess(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return getPipelineIfAllowedToAccess(user, projectId)
                .flatMap(Pipeline::getRunningStage);
    }

    @Nonnull
    private Optional<Pipeline> getPipelineIfAllowedToAccess(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                );
    }

    private Optional<Project> getProjectIfAllowedToAccess(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeAccessedBy(user));
    }

    @GetMapping("projects/{projectId}/stats")
    public Optional<Stats> getStats(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(winslow.getOrchestrator()::getRunningStageStats);
    }

    @PostMapping("projects/{projectId}/public")
    public ResponseEntity<Boolean> setPublic(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody String publicAccess) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive()
                .flatMap(container -> {
                    try (container) {
                        if (container.getNoThrow().filter(p -> p.canBeManagedBy(user)).isPresent()) {
                            var project = container.getNoThrow();
                            if (project.isPresent()) {
                                var p = project.get();
                                p.setPublic(Boolean.parseBoolean(publicAccess));
                                container.update(p);
                                return Optional.of(ResponseEntity.ok(p.isPublic()));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Optional.of(ResponseEntity.notFound().<Boolean>build());
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }


}
