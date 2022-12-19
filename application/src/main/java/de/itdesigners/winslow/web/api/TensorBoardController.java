package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Backoff;
import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.pipeline.*;
import de.itdesigners.winslow.web.ProxyRouting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class TensorBoardController {

    private static final @Nonnull Logger LOG = Logger.getLogger(TensorBoardController.class.getSimpleName());

    @Autowired
    private ProxyRouting routing;

    @Autowired
    private Winslow winslow;

    private final @Nonnull Map<String, ActiveBoard> activeBoards = new HashMap<>();
    private                int                      nextPort     = 50_100;

    public TensorBoardController() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var board : activeBoards.values()) {
                try (board.handle) {
                    board.handle.kill();
                } catch (IOException ioe) {
                    LOG.log(Level.SEVERE, "Unable to clean up active boards", ioe);
                }
            }
        }));
    }

    @GetMapping("/tensorboard/{projectId}/{stageId}/start")
    public ModelAndView start(
            HttpServletRequest request,
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId
    ) {
        try {
            return startAndMaybeThrow(request, user, projectId, stageId);
        } catch (Throwable t) {
            t.printStackTrace(); // prevent error propagation
            return new ModelAndView();
        }
    }

    public ModelAndView startAndMaybeThrow(
            HttpServletRequest request,
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId
    ) {
        var project = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> p.canBeAccessedBy(user))
                .orElseThrow();

        var pipeline = winslow
                .getOrchestrator()
                .getPipeline(project)
                .orElseThrow();


        var workDirConf = winslow.getWorkDirectoryConfiguration();
        var workspaces  = winslow.getResourceManager().getWorkspacesDirectory().orElseThrow().toAbsolutePath();
        var stage = pipeline
                .getActiveAndPastExecutionGroups()
                .flatMap(ExecutionGroup::getStages)
                .filter(s -> s.getFullyQualifiedId().equals(stageId))
                .findFirst();

        if (stage.isPresent()) {
            if (activeBoards.containsKey(projectId)) {
                var board = activeBoards.get(projectId);
                if (board.handle.hasFailed() || board.handle.hasFinished()) {
                    try (board.handle) {
                        LOG.warning("Previous TensorBoard instance has failed");
                        activeBoards.remove(projectId);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Failed to close handle", e);
                    }
                } else if (board.stageId.equals(stageId)) {
                    try {
                        return probeAvailableOrRetry(
                                request,
                                board.port,
                                board.destinationIp,
                                board.publicUrl
                        );
                    } catch (InterruptedException e) {
                        LOG.log(Level.WARNING, "Unable to retrieve requested data from remote", e);
                        return null;
                    }
                } else {
                    try (board.handle) {
                        LOG.info("Project has running TensorBoard but for wrong stageId, " + stageId + " != " + board.stageId);
                        activeBoards.remove(projectId);
                        board.handle.stop();
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Failed to stop running TensorBoard", e);
                    }
                }
            }


            var port          = getNextPort();
            var routePath     = Path.of("tensorboard", projectId);
            var routeLocation = ProxyRouting.getPublicLocation(routePath.toString());

            var volume = workDirConf
                    .getDockerVolumeConfiguration(workspaces.resolve(stage.get().getWorkspace().orElseThrow()))
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve exported path"));

            var submission = new Submission(
                    new StageId(
                            projectId,
                            0,
                            "tensorboard",
                            port
                    )
            )
                    .withHardwareRequirements(new Requirements(
                            null,
                            1024L,
                            null,
                            null
                    ))
                    .withExtension(new DockerVolumes(List.of(new DockerVolume(
                            "tensorboard-" + projectId + "-" + stageId + "-" + port,
                            volume.getType(),
                            "/data/",
                            volume.getTargetPath(),
                            volume.getOptions().orElse(""),
                            true
                    ))));


            var routeDestinationIp = prepareRoutingDestinationIp(submission, port);

            submission
                    .withExtension(new DockerPortMappings(
                            new DockerPortMappings.Entry(routeDestinationIp, port, port)
                    ))
                    .withExtension(new DockerImage(
                            "tensorflow/tensorflow:latest-gpu-py3",
                            new String[]{
                                    "tensorboard",
                                    "--logdir",
                                    "/data/",
                                    "--path_prefix=" + routeLocation + "/",
                                    "--port",
                                    String.valueOf(port),
                                    "--host",
                                    "0.0.0.0"
                            },
                            null,
                            false
                    ));

            var publicUrl = this.routing.addRoute(
                    routePath,
                    new ProxyRouting.Route(
                            "http://" + routeDestinationIp + ":" + port + routeLocation,
                            project::canBeAccessedBy
                    )
            );

            try {
                LOG.info("Starting new TensorBoard at " + routePath + ", internal port " + port + ", project " + projectId);
                var handle = winslow
                        .getOrchestrator()
                        .getBackends()
                        .filter(b -> b.isCapableOfExecuting(submission))
                        .findFirst()
                        .orElseThrow(() -> new IOException("No capable backend found"))
                        .submit(submission);

                this.activeBoards.put(projectId, new ActiveBoard(
                        projectId,
                        stageId,
                        port,
                        publicUrl,
                        routeDestinationIp,
                        handle
                ));

                var thread = new Thread(() -> {
                    try {
                        var backoff  = new Backoff(250, 950, 2f);
                        var iterator = handle.getLogs();
                        while (iterator.hasNext()) {
                            var entry = iterator.next();
                            if (entry == null) {
                                backoff.sleep();
                                backoff.grow();
                                continue;
                            } else {
                                backoff.reset();
                            }
                            LOG.log(
                                    entry.isError() ? Level.WARNING : Level.INFO,
                                    projectId + ", " + entry.getSource() + ": " + entry.getMessage()
                            );
                        }
                    } catch (IOException ioe) {
                        LOG.log(Level.SEVERE, "Failed to retrieve tensorboard logs", ioe);
                    }
                });
                thread.setName(projectId);
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                return probeAvailableOrRetry(request, port, routeDestinationIp, publicUrl);
            } catch (InterruptedException e) {
                e.printStackTrace();
                LOG.log(Level.WARNING, "Tensorboard is not reachable? Project " + projectId);
            }
        }

        return null;
    }

    private String prepareRoutingDestinationIp(@Nonnull Submission submission, int port) {
        // On the DEV-ENV Winslow runs on the host and has no container to which the
        // tensorboard can attach. In production mode, tensorboard can attach to winslow
        // and winslow can then access tensorboard by localhost. In host mode, the tensorboard
        // port must be exposed first
        return Optional
                .ofNullable(Env.getDevEnvIp())
                .orElseGet(() -> {
                    submission
                            .withExtension(new DockerContainerNetworkLinkage(
                                    Optional
                                            .ofNullable(System.getenv("HOSTNAME"))
                                            .orElse("winslow")
                            ));
                    return "127.0.0.1";
                });
    }

    private ModelAndView probeAvailableOrRetry(
            HttpServletRequest request,
            int port,
            String routeDestinationIp,
            String publicUrl) throws InterruptedException {
        if (probeAvailableRepeatedly(port, routeDestinationIp)) {
            return new ModelAndView("redirect:" + publicUrl + "/");
        }

        // try again
        return new ModelAndView("redirect:" + request.getRequestURI());
    }

    private boolean probeAvailableRepeatedly(int port, String routeDestinationIp) throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            try {
                try (var socket = new Socket(InetAddress.getByName(routeDestinationIp), port)) {
                    if (socket.isConnected()) {
                        return true;
                    }
                }
            } catch (IOException e) {
                Thread.sleep(20);
            }
        }
        return false;
    }

    private synchronized int getNextPort() {
        for (int i = 0; i < 1000; i++) {
            int port = nextPort++;
            try (var socket = new ServerSocket(port)) {
                socket.close();
                return port;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Port " + port + " already in use");
            }
        }
        throw new RuntimeException("Unable  to find a free port to start tensorboard on");
    }

    private record ActiveBoard(
            @Nonnull String projectId,
            @Nonnull String stageId,
            int port,
            @Nonnull String publicUrl,
            @Nonnull String destinationIp,
            @Nonnull StageHandle handle) {
    }
}
