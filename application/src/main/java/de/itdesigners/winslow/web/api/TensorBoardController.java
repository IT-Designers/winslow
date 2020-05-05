package de.itdesigners.winslow.web.api;

import com.hashicorp.nomad.apimodel.*;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.fs.NfsWorkDirectory;
import de.itdesigners.winslow.nomad.NomadBackend;
import de.itdesigners.winslow.nomad.SubmissionToNomadJobAdapter;
import de.itdesigners.winslow.pipeline.DockerNfsVolume;
import de.itdesigners.winslow.pipeline.DockerNfsVolumes;
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
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class TensorBoardController {

    @Autowired
    private ProxyRouting routing;

    @Autowired
    private Winslow winslow;

    private Map<String, ActiveBoard> activeBoards = new HashMap<>();
    private int                      nextPort     = 50_100;

    @GetMapping("/tensorboard/{projectId}/{stageId}/start")
    public ModelAndView start(
            HttpServletRequest request,
            @Nonnull User user,
            @PathVariable("projectId") String projectId,
            @PathVariable("stageId") String stageId
    ) {
        var project = winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(p -> ProjectsController.canUserAccessProject(user, p))
                .orElseThrow();

        var pipeline = winslow
                .getOrchestrator()
                .getPipeline(project)
                .orElseThrow();

        var backend = winslow.getOrchestrator().getBackend();
        var nomad   = backend instanceof NomadBackend ? ((NomadBackend) backend).getNewClient() : null;

        var workDirConf = winslow.getWorkDirectoryConfiguration();
        var nfsWorkDir  = workDirConf instanceof NfsWorkDirectory ? ((NfsWorkDirectory) workDirConf) : null;

        var workspaces = winslow.getResourceManager().getWorkspacesDirectory().orElseThrow().toAbsolutePath();
        var stage      = pipeline.getStage(stageId);

        if (nomad != null && nfsWorkDir != null && stage.isPresent()) {

            if (activeBoards.containsKey(projectId)) {
                var board = activeBoards.get(projectId);
                if (board.stageId.equals(stageId)) {
                    try {
                        return probeAvailableOrRetry(
                                request,
                                board.port,
                                board.destinationIp,
                                board.publicUrl
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    try {
                        nomad.getJobsApi().deregister(toNomadJobId(projectId));
                    } catch (IOException | NomadException e) {
                        e.printStackTrace();
                    }
                }
            }


            var task          = new Task();
            var port          = getNextPort();
            var id            = toNomadJobId(projectId, stageId);
            var routePath     = Path.of("tensorboard", projectId);
            var routeLocation = ProxyRouting.getPublicLocation(routePath.toString());

            task.setName("tensorboard");
            task.setDriver("docker");
            task.setConfig(new HashMap<>());
            task.getConfig().put("image", "tensorflow/tensorflow:latest-gpu-py3");
            task.getConfig().put(
                    "args",
                    List.of(
                            "tensorboard",
                            "--logdir",
                            "/data/",
                            "--path_prefix=" + routeLocation + "/",
                            "--port",
                            String.valueOf(port),
                            "--host",
                            "0.0.0.0"
                    )
            );
            String routeDestinationIp = prepareRoutingDestinationIp(task, port);


            SubmissionToNomadJobAdapter
                    .getDockerNfsVolumesConfigurer(task)
                    .accept(new DockerNfsVolumes(List.of(new DockerNfsVolume(
                            id + "-" + port,
                            "/data/",
                            nfsWorkDir.toExportedPath(workspaces.resolve(stage.get().getWorkspace().orElseThrow()))
                                      .orElseThrow(() -> new RuntimeException("Failed to retrieve exported path"))
                                      .toAbsolutePath()
                                      .toString(),
                            nfsWorkDir.getOptions(),
                            true
                    ))));

            var job = new Job()
                    .setId(id)
                    .addDatacenters("local")
                    .setType("batch")
                    .addTaskGroups(
                            new TaskGroup()
                                    .setName("task-group")
                                    .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                    .addTasks(task)
                    );

            try {
                nomad.getJobsApi().register(job);
                var publicUrl = this.routing.addRoute(
                        routePath,
                        new ProxyRouting.Route(
                                "http://" + routeDestinationIp + ":" + port + routeLocation,
                                u -> ProjectsController.canUserAccessProject(u, project)
                        )
                );

                this.activeBoards.put(projectId, new ActiveBoard(
                        projectId,
                        stageId,
                        port,
                        publicUrl,
                        routeDestinationIp
                ));

                return probeAvailableOrRetry(request, port, routeDestinationIp, publicUrl);

            } catch (IOException | NomadException | InterruptedException e) {
                e.printStackTrace();
                try {
                    nomad.getJobsApi().deregister(id);
                } catch (IOException | NomadException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        return null;
    }

    private String prepareRoutingDestinationIp(Task task, int port) {
        // On the DEV-ENV Winslow runs on the host and has no container to which the
        // tensorboard can attach. In production mode, tensorboard can attach to winslow
        // and winslow can then access tensorboard by localhost. In host mode, the tensorboard
        // port must be exposed first
        return Optional
                .ofNullable(Env.getDevEnvIp())
                .map(ip -> {
                    // 2020-04-29, soon-ish https://github.com/hashicorp/nomad/issues/646#issuecomment-596690053
                    task.setResources(new Resources());
                    task.getResources().addNetworks(
                            new NetworkResource().addReservedPorts(new Port().setValue(port))
                    );
                    return ip;
                })
                .orElseGet(() -> {
                    task
                            .getConfig()
                            .put(
                                    "network_mode",
                                    "container:" + Optional
                                            .ofNullable(System.getenv("HOSTNAME"))
                                            .orElse("winslow")
                            );
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
                Thread.sleep(1_000);
            }
        }
        return false;
    }

    private synchronized int getNextPort() {
        return nextPort++;
    }

    @Nonnull
    private static String toNomadJobId(@Nonnull String projectId, @Nonnull String stageId) {
        return "tensorboard-" + projectId + "-" + stageId;
    }

    private static class ActiveBoard {
        public final String projectId;
        public final String stageId;
        public final int    port;
        public final String publicUrl;
        public final String destinationIp;

        private ActiveBoard(
                String projectId,
                String stageId,
                int port,
                String publicUrl,
                String destinationIp) {
            this.projectId     = projectId;
            this.stageId       = stageId;
            this.port          = port;
            this.publicUrl     = publicUrl;
            this.destinationIp = destinationIp;
        }
    }
}
