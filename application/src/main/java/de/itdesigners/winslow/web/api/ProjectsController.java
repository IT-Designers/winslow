package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.*;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.pipeline.*;
import de.itdesigners.winslow.api.project.*;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.project.AuthTokens;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.web.*;
import de.itdesigners.winslow.web.api.noauth.PipelineTrigger;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class ProjectsController {

    private static final Logger LOG = Logger.getLogger(ProjectsController.class.getSimpleName());

    private final Winslow winslow;

    public ProjectsController(Winslow winslow) {
        this.winslow = winslow;
    }

    public Optional<ProjectInfo> getProject(@Nonnull User user, @Nonnull String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeAccessedBy(user))
                .map(ProjectInfoConverter::from);
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
    public Optional<ProjectInfo> createProject(User user, @RequestBody ProjectCreateRequest body) {
        return winslow
                .getPipelineRepository()
                .getPipeline(body.pipeline())
                .unsafe()
                .filter(pd -> pd.canBeAccessedBy(user))
                .flatMap(pipelineDefinition -> winslow
                        .getProjectRepository()
                        .createProject(user, pipelineDefinition, project -> {
                            project.setName(body.name());
                            body.optTags().ifPresent(tags -> {
                                project.setTags(tags.toArray(new String[0]));
                            });
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
    public Stream<ExecutionGroupInfo> getProjectHistory(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(this::getProjectHistory);
    }

    public Stream<ExecutionGroupInfo> getProjectHistoryUnchecked(@Nonnull String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .stream()
                .flatMap(this::getProjectHistory);
    }

    public Stream<ExecutionGroupInfo> getProjectHistory(@Nonnull Project project) {
        var pipeline = winslow.getOrchestrator().getPipeline(project);
        var active   = pipeline.stream().flatMap(Pipeline::getActiveExecutionGroups);
        var history  = pipeline.stream().flatMap(Pipeline::getExecutionHistory);

        return Stream.concat(
                history.map(g -> ExecutionGroupInfoConverter.convert(g, false, false)),
                active.map(g -> ExecutionGroupInfoConverter.convert(g, true, false))
        );
    }

    @GetMapping("/projects/{projectId}/history/reversed/{count}")
    public Stream<ExecutionGroupInfo> getPartialFromReversedHistory(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("count") int count) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project).stream())
                .flatMap(pipeline -> {
                    var history = pipeline
                            .getExecutionHistory()
                            .sequential()
                            .collect(Collectors.toList());

                    Collections.reverse(history);

                    return history
                            .stream()
                            .limit(count)
                            .map(g -> ExecutionGroupInfoConverter.convert(g, false, false));

                });
    }

    @GetMapping("/projects/{projectId}/history/reversed/{startGroupId}/{count}")
    public Stream<ExecutionGroupInfo> getPartialFromReversedHistoryStartingAt(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("startGroupId") String startGroupId,
            @PathVariable("count") int count) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project).stream())
                .flatMap(pipeline -> {
                    var history = pipeline
                            .getExecutionHistory()
                            .sequential()
                            .takeWhile(g -> !Objects.equals(g.getFullyQualifiedId(), startGroupId))
                            .collect(Collectors.toList());

                    Collections.reverse(history);

                    return history
                            .stream()
                            .limit(count)
                            .map(g -> ExecutionGroupInfoConverter.convert(g, false, false));

                });
    }

    @PostMapping("/projects/{projectId}/history/prune")
    public Stream<ExecutionGroupInfo> pruneProjectHistory(
            User user,
            @PathVariable("projectId") String projectId) throws IOException {
        var project = getProjectIfAllowedToAccess(user, projectId);
        if (project.isPresent()) {
            winslow.getOrchestrator().prunePipeline(project.get());
            return getProjectHistory(user, projectId);
        } else {
            return Stream.empty();
        }
    }

    @GetMapping("/projects/{projectId}/enqueued")
    public Stream<ExecutionGroupInfo> getEnqueued(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .stream()
                        .flatMap(Pipeline::getEnqueuedExecutions)
                )
                .map(g -> ExecutionGroupInfoConverter.convert(g, false, true));
    }

    @DeleteMapping("/projects/{projectId}/enqueued/{groupId}")
    public Optional<Boolean> deleteEnqueued(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("groupId") String groupId
    ) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .updatePipeline(project, pipeline -> pipeline.removeExecutionGroup(groupId).isPresent())
                );
    }

    @PutMapping("/projects/{projectId}/name")
    public ResponseEntity<String> setProjectName(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody String name
    ) throws LockException, IOException {
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

    @PutMapping("/projects/{projectId}/tags")
    public ResponseEntity<String[]> setProjectTags(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody String[] tags
    ) throws LockException, IOException {
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
    public Optional<PipelineDefinitionInfo> getProjectPipelineDefinition(
            User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(Project::getPipelineDefinitionId)
                .flatMap(id -> winslow.getPipelineRepository().getPipeline(id).unsafe())
                .map(PipelineDefinitionInfoConverter::from);
    }

    @PutMapping("projects/{projectId}/pipeline-definition")
    public ResponseEntity<PipelineDefinitionInfo> setProjectPipelineDefinition(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody PipelineDefinitionInfo pipeline) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive()
                .map(projectContainer -> {
                    try (projectContainer) {
                        try {

                            var updatedProject = projectContainer
                                    .get()
                                    .filter(project -> project.canBeManagedBy(user));

                            if (updatedProject.isPresent()) {
                                var project = updatedProject.get();
                                var handle = project.getPipelineDefinitionId().map(id -> winslow.getPipelineRepository().getPipeline(id));

                                if (handle.isEmpty()) {
                                    // TODO create pipeline definition, set it on the project
                                    throw new NotImplementedException("TODO create pipeline definition, set it on the project");
                                }

                                if (handle.isPresent()) {
                                    var lock = handle.get().exclusive();
                                    if (lock.isPresent()) {
                                        var container = lock.get();
                                        try (container) {
                                            var stored = PipelineDefinitionInfoConverter.reverse(pipeline);
                                            container.update(stored);
                                            return ResponseEntity.ok(PipelineDefinitionInfoConverter.from(stored));
                                        }
                                    }
                                }
                            }
                        } catch (LockException | IOException e) {
                            e.printStackTrace();
                        }

                        return ResponseEntity.notFound().<PipelineDefinitionInfo>build();
                    }

                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/projects/{projectId}/state")
    public Optional<State> getProjectState(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .flatMap(ProjectsController::getPipelineState));
    }

    private static Optional<State> getPipelineState(@Nonnull Pipeline pipeline) {
        return pipeline
                .getActiveExecutionGroups()
                .map(ExecutionGroup::getRunningStages)
                .flatMap(s -> {
                    if (s.findAny().isPresent()) {
                        return Stream.of(State.RUNNING);
                    } else {
                        return Stream.empty();
                    }
                })
                .findFirst()
                .or(() -> {
                    if (!pipeline.isPauseRequested() && pipeline
                            .getActiveExecutionGroups()
                            .map(g -> !g.isConfigureOnly() && g.getStages().findAny().isEmpty())
                            .findFirst()
                            .orElse(Boolean.FALSE)) {
                        return Optional.of(State.PREPARING);
                    } else {
                        return Optional.empty();
                    }
                })
                .or(() -> {
                    var mostRecent = pipeline
                            .getActiveOrPreviousExecutionGroup()
                            .flatMap(ExecutionGroup::getStages)
                            .reduce((first, second) -> second)
                            .filter(s -> s.getFinishTime().isPresent())
                            .map(Stage::getState);

                    if (mostRecent.isEmpty() && pipeline.isPauseRequested()) {
                        return Optional.of(State.PAUSED);
                    } else {
                        return mostRecent.map(state -> {
                            if (State.SUCCEEDED == state && pipeline.isPauseRequested()) {
                                return State.PAUSED;
                            } else {
                                return state;
                            }
                        });
                    }
                })
                .or(() -> {
                    if (!pipeline.isPauseRequested() && pipeline.hasEnqueuedStages()) {
                        return Optional.of(State.PREPARING);
                    } else {
                        return Optional.empty();
                    }
                });
    }

    @GetMapping("/projects/states")
    public Stream<StateInfo> getProjectComplexStates(User user, @RequestBody String[] projectIds) {
        return Stream
                .of(projectIds)
                .map(winslow.getProjectRepository()::getProject)
                .map(BaseRepository.Handle::unsafe)
                .map(p -> p
                        .filter(project -> project.canBeAccessedBy(user))
                        .flatMap(project -> winslow
                                .getOrchestrator()
                                .getPipeline(project))
                        .map(this::getStateInfo)
                        .orElse(null));
    }

    @Nonnull
    public StateInfo getStateInfo(@Nonnull Pipeline pipeline) {
        return getStateInfo(winslow, pipeline);
    }

    @Nonnull
    public static StateInfo getStateInfo(@Nonnull Winslow winslow, @Nonnull Pipeline pipeline) {
        var state = getPipelineState(pipeline).orElse(null);
        var expectedGroupSize = pipeline
                .getActiveExecutionGroups()
                .mapToLong(ExecutionGroup::getExpectedGroupSize)
                .sum();
        var runningGroupSize = pipeline.getActiveExecutionGroups().flatMap(ExecutionGroup::getRunningStages).count();
        var completedGroupSize = pipeline
                .getActiveExecutionGroups()
                .flatMap(ExecutionGroup::getCompletedStages)
                .count();
        var mostRecentStage = (String) null;

        if (expectedGroupSize != completedGroupSize) {
            if (expectedGroupSize == 1) {
                mostRecentStage = pipeline
                        .getActiveExecutionGroups()
                        .filter(g -> g.getRunningStages().findAny().isPresent())
                        .findFirst()
                        .map(ExecutionGroup::getStageDefinition)
                        .map(StageDefinition::name)
                        .orElse(null);
            } else if (expectedGroupSize > 1) {
                mostRecentStage = String.format(
                        "%d running, %d finished, %d expected",
                        runningGroupSize,
                        completedGroupSize,
                        expectedGroupSize
                );
            }
        }

        if (State.PREPARING == state) {
            mostRecentStage = "Searching a fitting execution node...";
        }


        return new StateInfo(
                state,
                pipeline
                        .getPauseReason()
                        .map(Pipeline.PauseReason::toString)
                        .orElse(null),
                mostRecentStage,
                pipeline.getActiveExecutionGroups()
                        .flatMap(group -> {
                            if (group.getExpectedGroupSize() > 1) {
                                var expected = group.getExpectedGroupSize();
                                var completed = (int) group
                                        .getStages()
                                        .filter(s -> s.getFinishState().isPresent())
                                        .count() * 100;
                                return Stream.of(completed / expected);
                            } else {
                                return group
                                        .getStages()
                                        .map(Stage::getFullyQualifiedId)
                                        .map(winslow.getRunInfoRepository()::getProgressHint)
                                        .flatMap(Optional::stream);
                            }
                        })
                        .findFirst()
                        .orElse(null),
                pipeline.hasEnqueuedStages()
        );
    }

    @PutMapping("projects/{projectId}/paused")
    public boolean setProjectNextStage(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody UpdatePauseRequest body) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    if (body.paused()) {
                        pipeline.requestPause();
                        return Boolean.TRUE;
                    } else {
                        if ("once".equalsIgnoreCase(body.strategy().orElse(null))) {
                            pipeline.resume(Pipeline.ResumeNotification.RunSingleThenPause);
                        } else {
                            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
                        }
                        return Boolean.FALSE;
                    }
                }))
                .orElse(Boolean.FALSE);
    }

    @Deprecated(forRemoval = true)
    @GetMapping("projects/{projectId}/paused")
    public boolean setProjectNextStage(User user, @PathVariable("projectId") String projectId) {
        LOG.log(Level.WARNING, "Someone accessed the deprecated /paused api");
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .map(Pipeline::isPauseRequested))
                .orElse(false);
    }

    @GetMapping("projects/{projectId}/logs/{stageId}")
    public Stream<LogEntryInfo> getProjectStageLogsLatest(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId,
            @RequestBody LogLinesRequest body) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                        .stream()
                        .flatMap(Pipeline::getActiveAndPastExecutionGroups)
                        .flatMap(ExecutionGroup::getStages)
                        .filter(stage -> {
                            if ("latest".equals(stageId)) {
                                return stage.getState() == State.RUNNING;
                            } else {
                                return stageId.equals(stage.getFullyQualifiedId());
                            }
                        })
                        .reduce((first, second) -> second)
                        .stream()
                        .flatMap(stage -> {
                            long skip = body
                                    .optSkipLines()
                                    .filter(line -> body
                                            .optExpectingStageId()
                                            .map(id -> Objects.equals(id, stage.getFullyQualifiedId()))
                                            .orElse(Boolean.TRUE)
                                    )
                                    .orElse(0L);

                            var line = new AtomicLong(skip);

                            return Stream.concat(
                                    winslow
                                            .getOrchestrator()
                                            .getLogs(
                                                    project,
                                                    stage.getFullyQualifiedId()
                                            )  // do not stream in parallel!
                                            .sequential()
                                            .skip(skip)
                                            .map(entry -> LogEntryInfoConverter.from(
                                                    entry,
                                                    line.incrementAndGet(),
                                                    stage.getFullyQualifiedId()
                                            )),
                                    Stream
                                            .of(0L)
                                            .flatMap(dummy -> {
                                                if (line.get() == 0L) {
                                                    return Stream.of(new LogEntryInfo(
                                                            System.currentTimeMillis(),
                                                            LogSource.MANAGEMENT_EVENT,
                                                            false,
                                                            "Loading...",
                                                            1L,
                                                            stage.getFullyQualifiedId() + "_temp"
                                                    ));
                                                } else {
                                                    return Stream.empty();
                                                }
                                            })
                            );
                        }));
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
                                                           stageId,
                                                           0L
                                                   ));
                    } catch (IOException e) {
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

    @PutMapping("projects/{projectId}/pipeline-definition-raw")
    public ResponseEntity<String> setProjectRawDefinition(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody String raw) throws IOException, LockException {

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

    @Deprecated(forRemoval = true)
    @GetMapping("projects/{projectId}/pause-reason")
    public Optional<Pipeline.PauseReason> getPauseReason(User user, @PathVariable("projectId") String projectId) {
        LOG.warning("Someone accessed the deprecated /paused-reason api");
        return getPipelineIfAllowedToAccess(user, projectId).flatMap(Pipeline::getPauseReason);
    }

    @GetMapping("projects/{projectId}/deletion-policy")
    public Optional<DeletionPolicy> getDeletionPolicy(User user, @PathVariable("projectId") String projectId) {
        return getPipelineIfAllowedToAccess(user, projectId).flatMap(Pipeline::getDeletionPolicy);
    }

    @GetMapping("projects/{projectId}/deletion-policy/default")
    public DeletionPolicy getDeletionPolicyDefault(User user, @PathVariable("projectId") String projectId) {
        return new DeletionPolicy();
    }

    @DeleteMapping("projects/{projectId}/deletion-policy")
    public ResponseEntity<Boolean> resetDeletionPolicy(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToManage(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    pipeline.setDeletionPolicy(null);
                    return ResponseEntity.ok(Boolean.TRUE); // just _some_ value
                }))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("projects/{projectId}/deletion-policy")
    public ResponseEntity<DeletionPolicy> setDeletionPolicyNumberOfWorkspacesOfSucceededStagesToKeep(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody DeletionPolicy policy) {
        return getProjectIfAllowedToManage(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    pipeline.setDeletionPolicy(policy);
                    return ResponseEntity.ok(policy); // just _some_ value
                }))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("projects/{projectId}/workspace-configuration-mode")
    public Optional<WorkspaceConfiguration.WorkspaceMode> getWorkspaceConfigurationMode(
            User user,
            @PathVariable("projectId") String projectId) {
        return getPipelineIfAllowedToAccess(user, projectId).flatMap(Pipeline::getWorkspaceConfigurationMode);
    }

    @PutMapping("projects/{projectId}/workspace-configuration-mode")
    public ResponseEntity<WorkspaceConfiguration.WorkspaceMode> setWorkspaceConfigurationMode(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody WorkspaceConfiguration.WorkspaceMode mode) {
        return getProjectIfAllowedToManage(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                    pipeline.setWorkspaceConfigurationMode(mode);
                    return ResponseEntity.ok(mode); // just _some_ value
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
                                    .withExecutionHistory(pipeline::getActiveAndPastExecutionGroups)
                                    .withEnqueuedStages(pipeline::getEnqueuedExecutions)
                                    .withInPipelineDefinitionDefinedVariables(
                                            project
                                                    .getPipelineDefinition()
                                                    .environment()
                                    );


                            try {
                                resolver = resolver.withGlobalVariables(
                                        winslow
                                                .getSettingsRepository()
                                                .getGlobalEnvironmentVariables()
                                );
                            } catch (IOException e) {
                                LOG.log(Level.WARNING, "Failed to load system requiredEnvVariables variables", e);
                            }

                            var stageDef = project
                                    .getPipelineDefinition()
                                    .stages()
                                    .stream()
                                    .skip(stageIndex)
                                    .findFirst();

                            if (stageDef.isPresent() && stageDef.get() instanceof StageWorkerDefinition) {
                                var workerStage = (StageWorkerDefinition) stageDef.get();
                                resolver = resolver
                                        .withIdAndStageName(stageDef.get().id(), stageDef.get().name())
                                        .withInStageDefinitionDefinedVariables(workerStage.environment());
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
                                .userInput()
                                .getRequiredEnvVariables().stream()
                        ,
                        project
                                .getPipelineDefinition()
                                .stages()
                                .stream()
                                .skip(stageIndex)
                                .findFirst()
                                .map(stage -> stage instanceof StageWorkerDefinition w
                                              ? w.userInput()
                                              : new UserInput(null, null))
                                .stream()
                                .flatMap(u -> u.getRequiredEnvVariables().stream())
                ));
    }

    @PutMapping("projects/{projectId}/pipeline/{pipelineId}")
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
                                            .filter(pd -> pd.canBeAccessedBy(user))
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

    @PostMapping("projects/{projectId}/enqueued")
    public void enqueueStageToExecute(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody EnqueueRequest body
    ) {
        getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> enqueueStageToExecute(project, body))
                .filter(v -> v)
                .orElseThrow();
    }

    @Nonnull
    public Optional<Boolean> enqueueStageToExecuteUnchecked(
            @Nonnull String projectId,
            @Nonnull EnqueueRequest request) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .flatMap(project -> enqueueStageToExecute(project, request));
    }

    @Nonnull
    public Optional<Boolean> enqueueStageToExecute(@Nonnull Project project, @Nonnull EnqueueRequest request) {
        return winslow.getOrchestrator().updatePipeline(project, pipeline -> {

            // not cloning it is fine, because it was loaded in unsafe-mode and only in this temporary scope
            // so changes will not be written back
            return getStageDefinitionNoClone(project, UUID.fromString(request.id()))
                    .map(stageDef -> {
                        enqueueExecutionStage(
                                pipeline,
                                stageDef,
                                request.env(),
                                request.rangedEnv(),
                                request.image(),
                                request.requiredResources(),
                                request.workspaceConfiguration(),
                                request.comment(),
                                request.optRunSingle().orElse(Boolean.FALSE),
                                request.optResume().orElse(Boolean.FALSE)
                        );
                        return Boolean.TRUE;
                    })
                    .orElse(Boolean.FALSE);
        });
    }

    @PostMapping("projects/{projectId}/enqueued-on-others")
    public Stream<Boolean> enqueueStageOnOthersToConfigure(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody EnqueueOnOtherRequest body
    ) {
        var stageDefinitionBase = getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().getPipeline(project).map(pipeline -> {
                    // not cloning it is fine, because opened in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    return getStageDefinitionNoClone(project, UUID.fromString(body.id()));
                }))
                .orElseThrow()
                .orElseThrow();

        return body
                .streamProjectIds()
                .map(id -> winslow.getProjectRepository().getProject(id).unsafe())
                .map(maybeProject -> maybeProject
                        .filter(project -> project.canBeAccessedBy(user))
                        .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                            enqueueConfigureStage(
                                    pipeline,
                                    stageDefinitionBase,
                                    body.env(),
                                    body.image(),
                                    body.requiredResources(),
                                    body.comment(),
                                    body.optRunSingle().orElse(Boolean.FALSE),
                                    body.optResume().orElse(Boolean.FALSE)
                            );
                            return Boolean.TRUE;
                        }))
                        .orElse(Boolean.FALSE));
    }

    @PostMapping("projects/{projectId}/action")
    public void enqueueAction(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody String actionId) {
        getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getPipelineRepository()
                        .getPipeline(actionId)
                        .unsafe()
                        .flatMap(pipelineDefinition -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {

                            for (var stage : pipelineDefinition.stages()) {

                                var stageToEnqueue = (stage instanceof StageWorkerDefinition workerDefinition) ?
                                                     new StageWorkerDefinitionBuilder()
                                                             .withTemplateBase(workerDefinition)
                                                             .withEnvironment(pipelineDefinition.environment())
                                                             .withAdditionalEnvironment(stage.environment())
                                                             .build()
                                                                                                               : stage;

                                pipeline.enqueueSingleExecution(
                                        stageToEnqueue,
                                        new WorkspaceConfiguration(
                                                pipeline
                                                        .getWorkspaceConfigurationMode()
                                                        .filter(m -> m == WorkspaceConfiguration.WorkspaceMode.STANDALONE || m == WorkspaceConfiguration.WorkspaceMode.INCREMENTAL)
                                                        .orElse(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL)
                                        ),
                                        null,
                                        null
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
            @Nullable ResourceInfo requiredResources,
            @Nullable String comment,
            boolean runSingle,
            boolean resume) {
        enqueueStage(
                pipeline,
                base,
                env,
                null,
                image,
                requiredResources,
                Action.CONFIGURE,
                null,
                comment,
                runSingle,
                resume
        );
    }

    private static void enqueueExecutionStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable Map<String, RangedValue> rangedEnv,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources,
            @Nullable WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment,
            boolean runSingle,
            boolean resume
    ) {
        enqueueStage(
                pipeline,
                base,
                env,
                rangedEnv,
                image,
                requiredResources,
                Action.EXECUTE,
                workspaceConfiguration,
                comment,
                runSingle,
                resume
        );
    }

    private static void enqueueStage(
            @Nonnull Pipeline pipeline,
            @Nonnull StageDefinition base,
            @Nonnull Map<String, String> env,
            @Nullable Map<String, RangedValue> rangedEnv,
            @Nonnull ImageInfo image,
            @Nonnull ResourceInfo requiredResources,
            @Nonnull Action action,
            @Nullable WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment,
            boolean runSingle,
            boolean resume
    ) {
        if (workspaceConfiguration == null) {
            workspaceConfiguration = new WorkspaceConfiguration();
        }

        var recentBase = Optional
                .of(base)
                .flatMap(def -> pipeline
                        .getActiveAndPastExecutionGroups()
                        .filter(g -> g.getStageDefinition().id().equals(def.id()))
                        .map(ExecutionGroup::getStageDefinition)
                        .reduce((first, second) -> second)
                )
                .orElse(base); // none before, so take the given as origin

        var resultDefinition = base;
        if (base instanceof StageWorkerDefinition stageWorkerBase) {
            var resultWorkerDefinition = createStageWorkerDefinition(
                    stageWorkerBase,
                    (StageWorkerDefinition) recentBase,
                    updatedResourceRequirement(stageWorkerBase.requirements(), requiredResources),
                    stageWorkerBase.userInput().withoutConfirmation(),
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
            maybeUpdateStageImageConfig(image, resultWorkerDefinition);
            resultWorkerDefinition.image().setShmSizeMegabytes(stageWorkerBase
                                                                       .image()
                                                                       .getShmSizeMegabytes()
                                                                       .orElse(null));
            resultDefinition = resultWorkerDefinition;
        }

        if (action == Action.CONFIGURE && resultDefinition instanceof StageWorkerDefinition workerDefinition) {
            pipeline.enqueueConfiguration(workerDefinition, comment);
        } else if (rangedEnv != null && !rangedEnv.isEmpty() && resultDefinition instanceof StageWorkerDefinition workerDefinition) {
            pipeline.enqueueRangedExecution(workerDefinition, workspaceConfiguration, rangedEnv);
        } else {
            pipeline.enqueueSingleExecution(resultDefinition, workspaceConfiguration, comment, null);
        }

        if (runSingle) {
            pipeline.clearPauseReason();
            pipeline.resume(Pipeline.ResumeNotification.RunSingleThenPause);
        } else if (resume) {
            pipeline.clearPauseReason();
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        } else {
            resumeIfPausedByStageFailure(pipeline);
            resumeIfWaitingForGoneStageConfirmation(pipeline);
            resumeIfPausedByNoFittingNodeFound(pipeline);
        }
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
                    update.cpus(),
                    update.megabytesOfRam(),
                    Optional.of(update.gpus())
                            .filter(count -> count > 0)
                            .map(gpus -> new Requirements.Gpu(
                                    gpus,
                                    Optional
                                            .ofNullable(requirements)
                                            .map(Requirements::getGpu)
                                            .flatMap(Requirements.Gpu::getVendor)
                                            .orElse(null),
                                    Optional
                                            .ofNullable(requirements)
                                            .map(Requirements::getGpu)
                                            .map(Requirements.Gpu::getSupport)
                                            .orElse(null)
                            ))
                            .orElse(null),
                    null
            );
        }
    }

    private static void resumeIfWaitingForGoneStageConfirmation(Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.ConfirmationRequired).equals(pipeline.getPauseReason())) {
            var noStageRequiresUserConfirmation = pipeline
                    .getEnqueuedExecutions()
                    .noneMatch(g -> getUserInputConfirmation(g.getStageDefinition()) != UserInput.Confirmation.NEVER);
            if (noStageRequiresUserConfirmation) {
                pipeline.resume(Pipeline.ResumeNotification.Confirmation);
            }
        }
    }

    public static UserInput.Confirmation getUserInputConfirmation(StageDefinition stagedef) {
        return stagedef instanceof StageWorkerDefinition w
               ? w.userInput().getConfirmation()
               : UserInput.Confirmation.NEVER;
    }

    private static void resumeIfPausedByStageFailure(@Nonnull Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.StageFailure).equals(pipeline.getPauseReason())) {
            pipeline.clearPauseReason();
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        }
    }

    private static void resumeIfPausedByNoFittingNodeFound(@Nonnull Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.NoFittingNodeFound).equals(pipeline.getPauseReason())) {
            pipeline.clearPauseReason();
            pipeline.resume(Pipeline.ResumeNotification.Confirmation);
        }
    }

    private static StageWorkerDefinition createStageWorkerDefinition(
            @Nonnull StageWorkerDefinition templateBase,
            @Nullable StageWorkerDefinition recentBase,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nonnull Map<String, String> env) {
        return new StageWorkerDefinitionBuilder()
                .withTemplateBase(templateBase)
                .withRecentBase(recentBase)
                .withRequirements(requirements)
                .withUserInput(requires)
                .withEnvironment(env)
                .build();
    }

    private static void maybeUpdateStageImageConfig(
            @Nonnull ImageInfo image,
            @Nonnull StageWorkerDefinition stageDef) {
        stageDef.image().setName(image.name());
        stageDef.image().setArgs(image.args());
        stageDef.image().setShmSizeMegabytes(image.shmMegabytes());
    }


    @DeleteMapping("projects/{projectId}")
    public ResponseEntity<String> delete(User user, @PathVariable("projectId") String projectId) {
        var project = getProjectIfAllowedToManage(user, projectId);
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
        if (project.getPipelineDefinition().stages().size() > index) {
            return Optional.of(project.getPipelineDefinition().stages().get(index));
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<StageDefinition> getStageDefinitionNoClone(@Nonnull Project project, UUID id) {
        return project.getPipelineDefinition().stages().stream().filter(s -> s.id().equals(id)).findFirst();
    }


    @PutMapping("projects/{projectId}/stop/{stageId}")
    public boolean stopSingleStageOrAllStagesOfActiveExecutionGroup(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable(value = "stageId", required = false) @Nullable String stageId,
            @RequestBody Boolean pause
    ) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(p -> winslow
                        .getOrchestrator()
                        .getPipeline(p)
                )
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .flatMap(g -> g
                        .getRunningStages()
                        .filter(s -> stageId == null || s.getFullyQualifiedId().equals(stageId))
                )
                .allMatch(stage -> {
                    try {
                        if (pause != null && pause) {
                            winslow.getOrchestrator().updatePipeline(
                                    getProjectIfAllowedToAccess(user, projectId).get(),
                                    pipeline -> {
                                        pipeline.requestPause();
                                        return null;
                                    }
                            );
                        }
                        winslow.getOrchestrator().stop(stage);
                        return true;
                    } catch (LockException e) {
                        LOG.log(Level.SEVERE, "Failed to stop stage " + stage.getFullyQualifiedId(), e);
                        return false;
                    }
                });
    }

    @PutMapping("projects/{projectId}/kill")
    public boolean killSingleStageOrAllStagesOfActiveExecutionGroup(
            User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow.getOrchestrator().updatePipeline(
                        project,
                        pipeline -> pipeline
                                .getActiveExecutionGroups().anyMatch(g -> {
                                    g.markAsCompleted();
                                    g.getRunningStages().forEach(stage -> {
                                        try {
                                            winslow.getOrchestrator().kill(stage);
                                        } catch (LockException e) {
                                            LOG.log(
                                                    Level.SEVERE,
                                                    "Failed to kill stage " + stage.getFullyQualifiedId(),
                                                    e
                                            );
                                        }
                                    });
                                    return Boolean.TRUE;
                                })
                ))
                .orElse(Boolean.FALSE);
    }

    @PutMapping("projects/{projectId}/kill/{stageId}")
    public boolean killSingleStageOrAllStagesOfActiveExecutionGroup(
            User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") @Nonnull String stageId) {
        return getPipelineIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(Pipeline::getActiveExecutionGroups)
                .map(ExecutionGroup::getRunningStages)
                .flatMap(r -> r.filter(s -> s.getFullyQualifiedId().equals(stageId)))
                .allMatch(stage -> {
                    try {
                        winslow.getOrchestrator().kill(stage);
                        return true;
                    } catch (LockException e) {
                        LOG.log(Level.SEVERE, "Failed to kill stage " + stage.getFullyQualifiedId(), e);
                        return false;
                    }
                });
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

    private Optional<Project> getProjectIfAllowedToManage(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeManagedBy(user));
    }

    @Deprecated(forRemoval = true)
    @GetMapping("projects/{projectId}/stats")
    public Stream<StatsInfo> getStats(User user, @PathVariable("projectId") String projectId) {
        LOG.warning("Someone accessed the deprecated /stats api");
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(winslow.getOrchestrator()::getRunningStageStats);
    }

    @PutMapping("projects/{projectId}/public")
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


    @GetMapping("projects/{projectId}/resource-limitation")
    public Optional<ResourceLimitation> getResourceLimitation(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId).flatMap(Project::getResourceLimitation);
    }

    @PutMapping("projects/{projectId}/resource-limitation")
    public ResponseEntity<ResourceLimitation> setResourceLimitation(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @RequestBody(required = false) ResourceLimitation limitation) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .exclusive()
                .flatMap(container -> {
                    try (container) {
                        return container.getNoThrow().filter(p -> p.canBeManagedBy(user)).map(project -> {
                            try {
                                project.setResourceLimitation(limitation);
                                container.update(project);
                                return ResponseEntity.ok(limitation);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return ResponseEntity.notFound().<ResourceLimitation>build();
                            }
                        });
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("projects/{projectId}/auth-tokens")
    public Stream<AuthTokenInfo> getAuthTokens(
            @Nonnull User user,
            @PathVariable("projectId") String projectId
    ) {
        return getProjectIfAllowedToManage(user, projectId)
                .stream()
                .flatMap(p -> {
                    var handle = winslow.getProjectAuthTokenRepository().getAuthTokens(projectId);
                    return handle.unsafe().stream().flatMap(AuthTokens::getTokens);
                })
                .map(AuthTokenInfoConverter::convert);
    }

    @PostMapping("projects/{projectId}/auth-tokens")
    public Optional<AuthTokenInfo> createAuthToken(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @RequestParam("name") String name
    ) {
        return getProjectIfAllowedToManage(user, projectId)
                .flatMap(p -> {
                    var handle = winslow.getProjectAuthTokenRepository().getAuthTokens(projectId);

                    return handle.exclusive().flatMap(exclusive -> {
                        try (exclusive) {
                            var tokens         = exclusive.get().orElseGet(() -> new AuthTokens(null));
                            var tokenAndSecret = tokens.createToken(name);
                            tokenAndSecret
                                    .getValue0()
                                    .addCapability(PipelineTrigger.REQUIRED_CAPABILITY_TRIGGER_PIPELINE);
                            exclusive.update(tokens);
                            return Optional.of(tokenAndSecret);
                        } catch (IOException | LockException e) {
                            LOG.log(Level.SEVERE, "Failed to update auth tokens for projectId=" + projectId, e);
                            return Optional.empty();
                        }
                    });
                })
                .map(tokenAndSecret -> AuthTokenInfoConverter
                        .convert(tokenAndSecret.getValue0())
                        .withSecret(tokenAndSecret.getValue1())
                );
    }

    @DeleteMapping("projects/{projectId}/auth-tokens/{tokenId}")
    public boolean deleteAuthToken(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("tokenId") String tokenId
    ) {
        return getProjectIfAllowedToManage(user, projectId)
                .flatMap(p -> {
                    var handle = winslow.getProjectAuthTokenRepository().getAuthTokens(projectId);
                    return handle.exclusive().map(exclusive -> {
                        try (exclusive) {
                            var tokens = exclusive.get().filter(e -> e.deleteTokenForId(tokenId).isPresent());
                            if (tokens.isPresent()) {
                                exclusive.update(tokens.get());
                            }
                            return tokens;
                        } catch (IOException | LockException e) {
                            LOG.log(Level.SEVERE, "Failed to delete an auth-token for projectId=" + projectId, e);
                            return Optional.empty();
                        }
                    });
                })
                .isPresent();
    }

    @GetMapping("projects/{projectId}/groups")
    public Stream<Link> getGroups(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> project.getGroups().stream());
    }

    @GetMapping("projects/{projectId}/groups/{group}")
    public Link getGroupLink(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("group") String group) {
        return getProjectIfAllowedToAccess(user, projectId)
                .map(project -> project
                        .getGroups()
                        .stream()
                        .filter(link -> link.name().equals(group))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "There is no such group for the given project"
                        )))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    @PostMapping("projects/{projectId}/groups")
    public void addOrUpdateGroup(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @RequestBody Link group) {
        var handle = winslow
                .getProjectRepository()
                .getProject(projectId);

        handle
                .unsafe()
                .filter(project -> project.canBeManagedBy(user))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        try (var exclusive = handle
                .exclusive()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))) {
            var project = exclusive.get().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
            project.addGroup(group);
            exclusive.update(project);
        } catch (IOException | LockException e) {
            LOG.log(Level.SEVERE, "Failed to addOrUpdateGroup on project=" + projectId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @DeleteMapping("projects/{projectId}/groups/{group}")
    public void removeGroup(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("group") String group) {
        var handle = winslow
                .getProjectRepository()
                .getProject(projectId);

        handle
                .unsafe()
                .filter(project -> project.canBeManagedBy(user))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));


        try (var exclusive = handle
                .exclusive()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))) {
            var project = exclusive.get().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
            if (!project.removeGroup(group)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "The group is not a member of the project"
                );
            }
            exclusive.update(project);
        } catch (IOException | LockException e) {
            LOG.log(Level.SEVERE, "Failed to removeGroup on project=" + projectId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
