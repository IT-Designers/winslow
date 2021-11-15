package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.*;
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
import de.itdesigners.winslow.web.AuthTokenInfoConverter;
import de.itdesigners.winslow.web.ExecutionGroupInfoConverter;
import de.itdesigners.winslow.web.PipelineInfoConverter;
import de.itdesigners.winslow.web.ProjectInfoConverter;
import de.itdesigners.winslow.web.api.noauth.PipelineTrigger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
                .getPipeline(body.pipeline)
                .unsafe()
                .flatMap(pipelineDefinition -> winslow
                        .getProjectRepository()
                        .createProject(user, pipelineDefinition, project -> {
                            project.setName(body.name);
                            if (body.tags != null && body.tags.size() > 0) {
                                project.setTags(body.tags.toArray(new String[0]));
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
    public Stream<ExecutionGroupInfo> getProjectHistory(User user, @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .stream()
                .flatMap(project -> {
                    var pipeline = winslow.getOrchestrator().getPipeline(project);
                    var active   = pipeline.stream().flatMap(Pipeline::getActiveExecutionGroups);
                    var history  = pipeline.stream().flatMap(Pipeline::getExecutionHistory);

                    return Stream.concat(
                            history.map(g -> ExecutionGroupInfoConverter.convert(g, false)),
                            active.map(g -> ExecutionGroupInfoConverter.convert(g, true))
                    );
                });
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
                            .map(g -> ExecutionGroupInfoConverter.convert(g, false));

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
                            .map(g -> ExecutionGroupInfoConverter.convert(g, false));

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
                .map(g -> ExecutionGroupInfoConverter.convert(g, false));
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
                        .flatMap(ProjectsController::getPipelineState));
    }

    private static Optional<State> getPipelineState(@Nonnull Pipeline pipeline) {
        return pipeline
                .getActiveExecutionGroups()
                .map(ExecutionGroup::getRunningStages)
                .flatMap(s -> {
                    if (s.findAny().isPresent()) {
                        return Stream.of(State.Running);
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
                        return Optional.of(State.Preparing);
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
                        return Optional.of(State.Paused);
                    } else {
                        return mostRecent.map(state -> {
                            if (State.Succeeded == state && pipeline.isPauseRequested()) {
                                return State.Paused;
                            } else {
                                return state;
                            }
                        });
                    }
                })
                .or(() -> {
                    if (!pipeline.isPauseRequested() && pipeline.hasEnqueuedStages()) {
                        return Optional.of(State.Preparing);
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
                        .map(StageDefinition::getName)
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

        if (State.Preparing == state) {
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
                    if (body.paused) {
                        pipeline.requestPause();
                        return Boolean.TRUE;
                    } else {
                        if ("once".equalsIgnoreCase(body.strategy)) {
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
                                return stage.getState() == State.Running;
                            } else {
                                return stageId.equals(stage.getFullyQualifiedId());
                            }
                        })
                        .reduce((first, second) -> second)
                        .stream()
                        .flatMap(stage -> {
                            var skip = body.skipLines != null ? body.skipLines : 0;
                            if (body.expectingStageId != null && !body.expectingStageId.equals(stage.getFullyQualifiedId())) {
                                skip = 0;
                            }

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
                                            .map(entry -> new LogEntryInfo(
                                                    line.incrementAndGet(),
                                                    stage.getFullyQualifiedId(),
                                                    entry
                                            )),
                                    Stream
                                            .of(0L)
                                            .flatMap(dummy -> {
                                                if (line.get() == 0L) {
                                                    return Stream.of(new LogEntryInfo(
                                                            1L,
                                                            stage.getFullyQualifiedId() + "_temp",
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

    @PutMapping("projects/{projectId}/deletion-policy")
    public ResponseEntity<DeletionPolicy> setDeletionPolicyNumberOfWorkspacesOfSucceededStagesToKeep(
            User user,
            @PathVariable("projectId") String projectId,
            @RequestBody DeletionPolicy policy) {
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
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeManagedBy(user))
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

                .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {

                    // not cloning it is fine, because it was loaded in unsafe-mode and only in this temporary scope
                    // so changes will not be written back
                    return getStageDefinitionNoClone(project, body.stageIndex)
                            .map(stageDef -> {
                                enqueueExecutionStage(
                                        pipeline,
                                        stageDef,
                                        body.env,
                                        body.rangedEnv,
                                        body.image,
                                        body.requiredResources,
                                        body.workspaceConfiguration,
                                        body.comment,
                                        body.runSingle != null && body.runSingle,
                                        body.resume != null && body.resume
                                );
                                return Boolean.TRUE;
                            })
                            .orElse(Boolean.FALSE);
                }))
                .filter(v -> v)
                .orElseThrow();
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
                    return getStageDefinitionNoClone(project, body.stageIndex);
                }))
                .orElseThrow()
                .orElseThrow();

        return Stream
                .of(body.projectIds)
                .map(id -> winslow.getProjectRepository().getProject(id).unsafe())
                .map(maybeProject -> maybeProject
                        .filter(project -> project.canBeAccessedBy(user))
                        .flatMap(project -> winslow.getOrchestrator().updatePipeline(project, pipeline -> {
                            enqueueConfigureStage(
                                    pipeline,
                                    stageDefinitionBase,
                                    body.env,
                                    body.image,
                                    body.requiredResources,
                                    body.comment,
                                    body.runSingle != null && body.runSingle,
                                    body.resume != null && body.resume
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

                            for (var stage : pipelineDefinition.getStages()) {
                                pipeline.enqueueSingleExecution(
                                        new StageDefinitionBuilder()
                                                .withTemplateBase(stage)
                                                .withEnvironment(pipelineDefinition.getEnvironment())
                                                .withAdditionalEnvironment(stage.getEnvironment())
                                                .build(),
                                        new WorkspaceConfiguration(
                                                pipeline
                                                        .getWorkspaceConfigurationMode()
                                                        .filter(m -> m == WorkspaceConfiguration.WorkspaceMode.STANDALONE || m == WorkspaceConfiguration.WorkspaceMode.INCREMENTAL)
                                                        .orElse(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL),
                                                null,
                                                null
                                        ),
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
                Action.Configure,
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
                Action.Execute,
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
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources,
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
                        .filter(g -> g.getStageDefinition().getName().equals(def.getName()))
                        .map(ExecutionGroup::getStageDefinition)
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
        maybeUpdateStageImageConfig(image, resultDefinition);
        base.getImage()
            .flatMap(Image::getShmSizeMegabytes)
            .ifPresent(shm -> resultDefinition.getImage().ifPresent(ri -> ri.setShmSizeMegabytes(shm)));

        if (action == Action.Configure) {
            pipeline.enqueueConfiguration(resultDefinition, comment);
        } else if (rangedEnv == null || rangedEnv.isEmpty()) {
            pipeline.enqueueSingleExecution(resultDefinition, workspaceConfiguration, comment);
        } else {
            pipeline.enqueueRangedExecution(resultDefinition, workspaceConfiguration, rangedEnv);
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

    private static void resumeIfWaitingForGoneStageConfirmation(Pipeline pipeline) {
        if (Optional.of(Pipeline.PauseReason.ConfirmationRequired).equals(pipeline.getPauseReason())) {
            var noStageRequiresUserConfirmation = pipeline
                    .getEnqueuedExecutions()
                    .noneMatch(g -> g
                            .getStageDefinition()
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

    private static void maybeUpdateStageImageConfig(
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

    @Deprecated(forRemoval = true)
    @GetMapping("projects/{projectId}/stats")
    public Stream<Stats> getStats(User user, @PathVariable("projectId") String projectId) {
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
                        return container.getNoThrow().filter(p -> p.canBeAccessedBy(user)).map(project -> {
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
        return getProjectIfAllowedToAccess(user, projectId)
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
        return getProjectIfAllowedToAccess(user, projectId)
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
                .map(tokenAndSecret -> AuthTokenInfoConverter.convert(tokenAndSecret.getValue0())
                                                             .withSecret(tokenAndSecret.getValue1()));
    }

    @DeleteMapping("projects/{projectId}/auth-tokens/{tokenId}")
    public boolean deleteAuthToken(
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("tokenId") String tokenId
    ) {
        return getProjectIfAllowedToAccess(user, projectId)
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
}
