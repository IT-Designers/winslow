package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.StateInfo;
import de.itdesigners.winslow.api.project.ProjectInfo;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.ProjectInfoConverter;
import de.itdesigners.winslow.web.api.ProjectsController;
import de.itdesigners.winslow.web.websocket.ChangeEvent.ChangeType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Controller
public class ProjectsEndpointController {

    public static final @Nonnull String TOPIC_PREFIX                 = "/projects";
    public static final @Nonnull String TOPIC_PROJECTS               = TOPIC_PREFIX;
    public static final @Nonnull String TOPIC_PROJECT_STATES         = TOPIC_PREFIX + "/states";
    public static final @Nonnull String TOPIC_PROJECT_SPECIFIC_STATS = TOPIC_PREFIX + "/%s/stats";

    private final @Nonnull MessageSender      sender;
    private final @Nonnull Winslow            winslow;
    private final @Nonnull ProjectsController projects;

    private final @Nonnull Map<String, RunningProjectsEndpointPublisher> runningPublishers = new ConcurrentHashMap<>();

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
            while (true) {
                var last = System.currentTimeMillis();
                this.runningPublishers.values().forEach(Pollable::poll);
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

    private void onProjectRelease(@Nonnull String projectId, @Nullable Project project) {
        if (project == null) {
            stopProjectPublisher(projectId);
            publishProjectUpdate(TOPIC_PROJECTS, projectId, Collections.singletonList(null), project);
        } else {
            publishProjectUpdate(
                    TOPIC_PROJECTS,
                    projectId,
                    Collections.singletonList(ProjectInfoConverter.from(project)),
                    project
            );
        }
    }

    private void onPipelineRelease(@Nonnull String projectId, @Nullable Pipeline pipeline) {
        if (pipeline == null) {
            stopProjectPublisher(projectId);
            publishProjectUpdate(TOPIC_PROJECT_STATES, projectId, Collections.singletonList(null), null);
        } else {
            this.winslow.getProjectRepository().getProject(projectId).unsafe().ifPresent(project -> {
                var info = projects.getStateInfo(pipeline);
                createOrStopProjectPublisher(projectId, project, State.Running == info.state);
                publishProjectUpdate(TOPIC_PROJECT_STATES, projectId, Collections.singletonList(info), project);
            });
        }
    }

    private void createOrStopProjectPublisher(
            @Nonnull String projectId,
            @Nonnull Project project,
            boolean shouldBeRunning) {
        if (shouldBeRunning) {
            this.runningPublishers.computeIfAbsent(
                    projectId,
                    _pid -> new RunningProjectsEndpointPublisher(sender, winslow, project)
            ).updateProject(project);
        } else {
            stopProjectPublisher(projectId);
        }
    }

    private void stopProjectPublisher(@Nonnull String projectId) {
        Optional.ofNullable(this.runningPublishers.remove(projectId)).ifPresent(Pollable::pollAndClose);
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

    @Nonnull
    public Optional<User> getUser(@Nullable Principal principal) {
        return getUser(winslow, principal);
    }

    @Nonnull
    public static Optional<User> getUser(@Nonnull Winslow winslow, @Nullable Principal principal) {
        var userName = Env.isDevEnv()
                       ? Optional.ofNullable(System.getenv(Env.DEV_REMOTE_USER))
                       : Optional.<String>empty();
        return userName
                .or(() -> Optional.ofNullable(principal).map(Principal::getName))
                .flatMap(winslow.getUserRepository()::getUserOrCreateAuthenticated);
    }

    @Nonnull
    public static PrincipalPermissionChecker getPermissionChecker(@Nonnull Winslow winslow, @Nullable Project project) {
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
}
