package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ExecutionGroupInfo;
import de.itdesigners.winslow.api.pipeline.LogEntryInfo;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.StateInfo;
import de.itdesigners.winslow.api.project.ProjectInfo;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.ExecutionGroupInfoConverter;
import de.itdesigners.winslow.web.ProjectInfoConverter;
import de.itdesigners.winslow.web.api.ProjectsController;
import de.itdesigners.winslow.web.websocket.ChangeEvent.ChangeType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class ProjectsEndpointController {

    private static final @Nonnull Logger LOG = Logger.getLogger(ProjectsEndpointController.class.getSimpleName());

    public static final @Nonnull String TOPIC_PREFIX                       = "/projects";
    public static final @Nonnull String TOPIC_PROJECTS                     = TOPIC_PREFIX;
    public static final @Nonnull String TOPIC_PROJECT_STATES               = TOPIC_PREFIX + "/states";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_STATS       = TOPIC_PREFIX + "/%s/stats";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_LOGS_LATEST = TOPIC_PREFIX + "/%s/logs/latest";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_LOGS_STAGE  = TOPIC_PREFIX + "/%s/logs/%s";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_HISTORY     = TOPIC_PREFIX + "/%s/history";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_EXECUTING   = TOPIC_PREFIX + "/%s/executing";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_ENQUEUED    = TOPIC_PREFIX + "/%s/enqueued";
    public static final          int    MAX_LOG_ENTRIES                    = 1024;
    public static final          int    ON_SUBSCRIBE_HISTORY_COUNT         = 10;

    private final @Nonnull MessageSender      sender;
    private final @Nonnull Winslow            winslow;
    private final @Nonnull ProjectsController projects;

    private final @Nonnull Map<String, CoolDownWrapper<RunningProjectsEndpointPublisher>> runningPublishers = new ConcurrentHashMap<>();

    // TODO missing cache cleanup
    private final @Nonnull Map<String, Object> cache = new ConcurrentHashMap<>();

    public ProjectsEndpointController(
            @Nonnull SimpMessagingTemplate simp,
            @Nonnull Winslow winslow,
            @Nonnull ProjectsController projects) {
        this.sender   = new MessageSender(simp);
        this.winslow  = winslow;
        this.projects = projects;


        this.winslow
                .getProjectRepository()
                .registerProjectChangeListener(pair -> onProjectRelease(
                        pair.getValue0(),
                        pair.getValue1().unsafe().orElse(null)
                ));

        this.winslow
                .getOrchestrator()
                .getPipelines()
                .registerPipelineChangeListener(pair -> onPipelineRelease(
                        pair.getValue0(),
                        pair.getValue1().unsafe().orElse(null)
                ));

        startPollDaemon();
    }

    private void startPollDaemon() {
        var thread = new Thread(() -> {
            winslow
                    .getOrchestrator()
                    .getProjects()
                    .getProjects()
                    .flatMap(handle -> handle.unsafe().stream())
                    .filter(project -> winslow
                            .getOrchestrator()
                            .getPipeline(project)
                            .flatMap(pipeline -> pipeline
                                    .getActiveExecutionGroup()
                                    .map(g -> g.getStages()
                                               .anyMatch(stage -> stage.getState() == State.Running))
                            )
                            .orElse(Boolean.FALSE)
                    )
                    .forEach(project -> createOrStopProjectPublisher(project.getId(), project, true));

            while (true) {
                var last = System.currentTimeMillis();
                var completed = this.runningPublishers
                        .entrySet()
                        .stream()
                        .peek(e -> e.getValue().poll())
                        .filter(e -> e.getValue().hasCompleted())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                completed
                        .stream()
                        .peek(stage -> LOG.info("Publisher for " + stage + " has stopped"))
                        .map(this.runningPublishers::remove)
                        .forEach(Pollable::pollAndClose);

                var diff = 1_000 - (System.currentTimeMillis() - last);
                LockBus.ensureSleepMs(Math.max(100, diff));
            }
        });

        thread.setName(getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    private void publishProjectUpdate(
            @Nonnull String topic,
            @Nonnull String projectId,
            @Nullable Object value,
            @Nullable Project project) {
        this.sender.publishProjectUpdate(winslow, topic, projectId, value, project);
    }

    private void publishProjectUpdateCached(
            @Nonnull String topic,
            @Nonnull String projectId,
            @Nullable Object value,
            @Nullable Project project) {
        var prev = this.cache.put(topic, value);
        if (!Objects.equals(prev, value)) {
            this.sender.publishProjectUpdate(winslow, topic, projectId, value, project);
        }

    }

    private void publishProjectUpdateCachedDelta(
            @Nonnull String topic,
            @Nonnull String projectId,
            @Nullable Object value,
            @Nullable Project project) {
        var prev = this.cache.put(topic, value);
        if (prev instanceof Collection<?> && value instanceof Collection<?>) {
            var prevSize = ((Collection<?>) prev).size();
            var currSize = ((Collection<?>) value).size();
            if (prevSize <= currSize) {
                // send only then new entries
                this.sender.publishProjectUpdate(
                        winslow,
                        topic,
                        projectId,
                        ((Collection<?>) value)
                                .stream()
                                .skip(prevSize)
                                .collect(Collectors.toList()),
                        project
                );
            } else {
                this.sender.publishProjectUpdate(winslow, topic, projectId, value, project);
            }
        } else if (!Objects.equals(prev, value)) {
            this.sender.publishProjectUpdate(winslow, topic, projectId, value, project);
        }

    }

    private void onProjectRelease(@Nonnull String projectId, @Nullable Project project) {
        if (project == null) {
            stopProjectPublisher(projectId);
            publishProjectUpdate(TOPIC_PROJECTS, projectId, Collections.singletonList(null), null);
        } else {
            publishProjectUpdate(
                    TOPIC_PROJECTS,
                    projectId,
                    ProjectInfoConverter.from(project),
                    project
            );
        }
    }

    private void onPipelineRelease(@Nonnull String projectId, @Nullable Pipeline pipeline) {
        if (pipeline == null) {
            stopProjectPublisher(projectId);
            publishProjectUpdate(TOPIC_PROJECT_STATES, projectId, null, null);
            publishProjectUpdateCachedDelta(
                    String.format(TOPIC_PROJECT_SPECIFIC_HISTORY, projectId),
                    projectId,
                    Collections.singletonList(null),
                    null
            );
            publishProjectUpdateCached(
                    String.format(TOPIC_PROJECT_SPECIFIC_EXECUTING, projectId),
                    projectId,
                    Collections.singletonList(null),
                    null
            );
            publishProjectUpdateCached(
                    String.format(TOPIC_PROJECT_SPECIFIC_ENQUEUED, projectId),
                    projectId,
                    Collections.singletonList(null),
                    null
            );
        } else {
            this.winslow.getProjectRepository().getProject(projectId).unsafe().ifPresent(project -> {
                var info = projects.getStateInfo(pipeline);
                createOrStopProjectPublisher(projectId, project, State.Running == info.state);
                publishProjectUpdate(TOPIC_PROJECT_STATES, projectId, info, project);
                publishProjectUpdateCachedDelta(
                        String.format(TOPIC_PROJECT_SPECIFIC_HISTORY, projectId),
                        projectId,
                        getHistoryInfo(pipeline),
                        project
                );
                publishProjectUpdateCached(
                        String.format(TOPIC_PROJECT_SPECIFIC_EXECUTING, projectId),
                        projectId,
                        getExecutionInfo(pipeline),
                        project
                );
                publishProjectUpdateCached(
                        String.format(TOPIC_PROJECT_SPECIFIC_ENQUEUED, projectId),
                        projectId,
                        getEnqueuedInfo(pipeline),
                        project
                );
            });
        }
    }

    @Nonnull
    private List<ExecutionGroupInfo> getEnqueuedInfo(@Nonnull Pipeline pipeline) {
        return pipeline
                .getEnqueuedExecutions()
                .map(g -> ExecutionGroupInfoConverter.convert(g, false, true))
                .collect(Collectors.toList());
    }

    @Nonnull
    private List<ExecutionGroupInfo> getExecutionInfo(@Nonnull Pipeline pipeline) {
        return pipeline
                .getActiveExecutionGroup()
                .map(g -> ExecutionGroupInfoConverter.convert(g, true, false))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Nonnull
    private List<ExecutionGroupInfo> getHistoryInfo(@Nonnull Pipeline pipeline) {
        return pipeline
                .getExecutionHistory()
                .map(g -> ExecutionGroupInfoConverter.convert(g, false, false))
                .collect(Collectors.toList());
    }

    private void createOrStopProjectPublisher(
            @Nonnull String projectId,
            @Nonnull Project project,
            boolean shouldBeRunning) {
        if (shouldBeRunning) {
            this.runningPublishers
                    .computeIfAbsent(
                            projectId,
                            _pid -> new CoolDownWrapper<>(new RunningProjectsEndpointPublisher(
                                    sender,
                                    winslow,
                                    project
                            ))
                    )
                    .value
                    .updateProject(project);
            LOG.info("Publisher for " + projectId + " has been started");
        } else {
            stopProjectPublisher(projectId);
            LOG.info("Publisher for " + projectId + " has been requested to stop");
        }
    }

    private void stopProjectPublisher(@Nonnull String projectId) {
        Optional
                .ofNullable(this.runningPublishers.get(projectId))
                .ifPresent(CoolDownWrapper::startCooldown);
    }

    @SubscribeMapping("/projects")
    public Stream<ChangeEvent<String, ProjectInfo>> subscribeProjects(Principal principal) throws Exception {
        return getUser(principal)
                .stream()
                .flatMap(projects::listProjects)
                .map(p -> new ChangeEvent<>(ChangeType.CREATE, p.id, p));
    }

    @SubscribeMapping("/projects/states")
    public Stream<ChangeEvent<String, StateInfo>> subscribeProjectStates(Principal principal) throws Exception {
        return getUser(principal)
                .map(user -> projects
                        .listProjects(user)
                        .flatMap(project -> winslow
                                .getOrchestrator()
                                .getPipeline(project.id)
                                .map(projects::getStateInfo)
                                .map(state -> new ChangeEvent<>(ChangeType.CREATE, project.id, state))
                                .stream()
                        ))
                .orElse(Stream.empty());
    }

    @SubscribeMapping("/projects/{projectId}/history")
    public Stream<ChangeEvent<String, List<ExecutionGroupInfo>>> subscribeProjectHistory(
            @DestinationVariable("projectId") String projectId,
            Principal principal) {
        return getUser(principal)
                .filter(user -> projects.getProject(user, projectId).isPresent())
                .flatMap(user -> winslow.getOrchestrator().getPipeline(projectId))
                .map(pipeline -> {
                    var historyInfo = getHistoryInfo(pipeline);
                    this.cache.put(String.format(TOPIC_PROJECT_SPECIFIC_HISTORY, projectId), historyInfo);
                    var length = Math.max(0, historyInfo.size() - ON_SUBSCRIBE_HISTORY_COUNT);
                    return new ChangeEvent<>(
                            ChangeType.CREATE,
                            projectId,
                            historyInfo.stream().skip(length).collect(Collectors.toList())
                    );
                })
                .stream();
    }

    @SubscribeMapping("/projects/{projectId}/executing")
    public Stream<ChangeEvent<String, List<ExecutionGroupInfo>>> subscribeProjectExecution(
            @DestinationVariable("projectId") String projectId,
            Principal principal) {
        return getUser(principal)
                .filter(user -> projects.getProject(user, projectId).isPresent())
                .flatMap(user -> winslow.getOrchestrator().getPipeline(projectId))
                .map(pipeline -> {
                    var execInfo = getExecutionInfo(pipeline);
                    this.cache.put(String.format(TOPIC_PROJECT_SPECIFIC_EXECUTING, projectId), execInfo);
                    return new ChangeEvent<>(ChangeType.CREATE, projectId, execInfo);
                })
                .stream();
    }

    @SubscribeMapping("/projects/{projectId}/enqueued")
    public Stream<ChangeEvent<String, List<ExecutionGroupInfo>>> subscribeProjectEnqueued(
            @DestinationVariable("projectId") String projectId,
            Principal principal) {
        return getUser(principal)
                .filter(user -> projects.getProject(user, projectId).isPresent())
                .flatMap(user -> winslow.getOrchestrator().getPipeline(projectId))
                .map(pipeline -> {
                    var enqueuedInfo = getEnqueuedInfo(pipeline);
                    this.cache.put(String.format(TOPIC_PROJECT_SPECIFIC_ENQUEUED, projectId), enqueuedInfo);
                    return new ChangeEvent<>(ChangeType.CREATE, projectId, enqueuedInfo);
                })
                .stream();
    }

    @SubscribeMapping("/projects/{projectId}/logs/latest")
    public Stream<ChangeEvent<String, List<LogEntryInfo>>> subscribeLogsLatest(
            @DestinationVariable("projectId") String projectId,
            Principal principal) {
        return Stream.of(
                getUser(principal)
                        .filter(user -> projects.getProject(user, projectId).isPresent())
                        .flatMap(user -> Optional
                                .ofNullable(this.runningPublishers.get(projectId))
                                .map(w -> w.value.getLogEntryLatestUpToHead(MAX_LOG_ENTRIES))
                        )
                        .map(logs -> new ChangeEvent<>(ChangeType.CREATE, projectId, logs))
                        .orElseGet(() -> new ChangeEvent<>(
                                ChangeType.CREATE,
                                projectId,
                                RunningProjectsEndpointPublisher.getLogEntryLatestUpToHead(
                                        winslow,
                                        projectId,
                                        id -> Long.MAX_VALUE,
                                        MAX_LOG_ENTRIES
                                )
                        ))
        );
    }

    @SubscribeMapping("/projects/{projectId}/logs/{stageId}")
    public Stream<ChangeEvent<String, List<LogEntryInfo>>> subscribeLogsLatestForStage(
            @DestinationVariable("projectId") String projectId,
            @DestinationVariable("stageId") String stageId,
            Principal principal) {
        return Stream.of(
                getUser(principal)
                        .filter(user -> projects.getProject(user, projectId).isPresent())
                        .flatMap(user -> Optional
                                .ofNullable(this.runningPublishers.get(projectId))
                                .map(w -> w.value.getLogEntryUpToHead(stageId, MAX_LOG_ENTRIES))
                        )
                        .map(logs -> new ChangeEvent<>(ChangeType.CREATE, projectId, logs))
                        .orElseGet(() -> new ChangeEvent<>(
                                ChangeType.CREATE,
                                projectId,
                                RunningProjectsEndpointPublisher.getLogEntryUpToHead(
                                        winslow,
                                        projectId,
                                        id -> Long.MAX_VALUE,
                                        stageId,
                                        MAX_LOG_ENTRIES
                                )
                        ))
        );
    }

    @Nonnull
    public Optional<User> getUser(@Nullable Principal principal) {
        return getUser(winslow, principal);
    }

    @Nonnull
    public static Optional<User> getUser(@Nonnull Winslow winslow, @Nullable Principal principal) {
        return getUser(winslow, Optional.ofNullable(principal).map(Principal::getName).orElse(null));
    }

    public static Optional<User> getUser(@Nonnull Winslow winslow, @Nullable String user) {
        return Env.getDevUser()
                  .or(() -> Optional.ofNullable(user))
                  .flatMap(winslow.getUserRepository()::getUserOrCreateAuthenticated);
    }

    @Nonnull
    public static PrincipalPermissionChecker getPermissionChecker(@Nonnull Winslow winslow, @Nullable Project project) {
        LOG.finer("PrincipalPermissionChecker checks for canBeAccessedBy=" + (project != null));
        return project != null
               ? p -> ProjectsEndpointController.getUser(winslow, p).filter(project::canBeAccessedBy).isPresent()
               : p -> ProjectsEndpointController.getUser(winslow, p).isPresent();
    }

    public static <T, V> ChangeEvent<T, V> encapsulate(@Nonnull T identifier, @Nullable V value) {
        return new ChangeEvent<T, V>(
                value != null ? ChangeEvent.ChangeType.UPDATE : ChangeEvent.ChangeType.DELETE,
                identifier,
                value
        );
    }

    private static class CoolDownWrapper<T extends Pollable> implements Pollable {
        public                int counter = -1;
        public final @Nonnull T   value;

        private CoolDownWrapper(@Nonnull T value) {
            this.value = value;
        }

        public boolean hasCompleted() {
            return counter == 0;
        }

        public void startCooldown() {
            if (this.counter < 0) {
                this.counter = 3;
            }
        }

        @Override
        public void poll() {
            this.value.poll();
            if (this.counter > 0) {
                this.counter -= 1;
            }
        }

        @Override
        public void close() {
            this.value.close();
        }
    }
}
