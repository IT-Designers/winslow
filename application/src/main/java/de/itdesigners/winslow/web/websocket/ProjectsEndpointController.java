package de.itdesigners.winslow.web.websocket;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.StateInfo;
import de.itdesigners.winslow.api.project.ProjectInfo;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.web.ProjectInfoConverter;
import de.itdesigners.winslow.web.api.ProjectsController;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
public class ProjectsEndpointController {

    private final @Nonnull MessageSender      sender;
    private final @Nonnull Winslow            winslow;
    private final @Nonnull ProjectsController projects;

    public ProjectsEndpointController(
            @Nonnull SimpMessagingTemplate simp,
            @Nonnull Winslow winslow,
            @Nonnull ProjectsController projects) {
        this.sender   = new MessageSender(simp);
        this.winslow  = winslow;
        this.projects = projects;


        this.winslow.getProjectRepository().registerProjectChangeListener(pair -> {
            pair.getValue1().unsafe().ifPresentOrElse(project -> {
                this.sender.convertAndSend(
                        "/projects",
                        Collections.singletonList(new ChangeEvent<>(
                                ChangeEvent.ChangeType.UPDATE,
                                project.getId(),
                                ProjectInfoConverter.from(project)
                        )),
                        principal -> getUser(principal).filter(project::canBeAccessedBy).isPresent()
                );
            }, () -> this.sender.convertAndSend(
                    "/projects",
                    Collections.singletonList(new ChangeEvent<>(
                            ChangeEvent.ChangeType.DELETE,
                            pair.getValue0(),
                            null
                    )),
                    principal -> getUser(principal).isPresent()
            ));
        });

        this.winslow.getOrchestrator().getPipelines().registerPipelineChangeListener(pair -> {
            pair.getValue1().unsafe().ifPresentOrElse(pipeline -> {
                this.winslow.getProjectRepository().getProject(pipeline.getProjectId()).unsafe().ifPresent(project -> {
                    this.sender.convertAndSend(
                            "/projects/states",
                            Collections.singletonList(new ChangeEvent<>(
                                    ChangeEvent.ChangeType.UPDATE,
                                    project.getId(),
                                    projects.getStateInfo(pipeline)
                            )),
                            principal -> getUser(principal).filter(project::canBeAccessedBy).isPresent()
                    );
                });
            }, () -> this.sender.convertAndSend(
                    "/projects/states",
                    Collections.singletonList(new ChangeEvent<>(
                            ChangeEvent.ChangeType.DELETE,
                            pair.getValue0(),
                            null
                    )),
                    principal -> getUser(principal).isPresent()
            ));
        });

    }

    @SubscribeMapping("/projects")
    public Stream<ChangeEvent<String, ProjectInfo>> subscribeProjects(Principal principal) throws Exception {
        return getUser(principal)
                .stream()
                .flatMap(projects::listProjects)
                .map(p -> new ChangeEvent<>(ChangeEvent.ChangeType.CREATE, p.id, p));
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
                                .map(state -> new ChangeEvent<>(ChangeEvent.ChangeType.CREATE, project.id, state))
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
}
