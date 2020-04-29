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
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class TensorBoardController {

    @Autowired
    private ProxyRouting routing;

    @Autowired
    private Winslow winslow;

    private Set<String> activeBoards = new HashSet<>();
    private int         nextPort     = 50_100;

    @GetMapping("/tensorboard/{projectId}/{stageId}/start")
    public ModelAndView start(
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
        var publicIp   = Env.getPublicIp(); // TODO
        var stage      = pipeline.getStage(stageId);

        if (nomad != null && nfsWorkDir != null && stage.isPresent() && publicIp != null) {

            if (activeBoards.remove(projectId)) {
                try {
                    nomad.getJobsApi().deregister(toNomadJobId(projectId));
                } catch (IOException | NomadException e) {
                    e.printStackTrace();
                }
            }


            var task          = new Task();
            var port          = getNextPort();
            var id            = toNomadJobId(projectId);
            var routePath     = Path.of("tensorboard", projectId);
            var routeLocation = ProxyRouting.getPublicLocation(routePath.toString());

            task.setName("task-main");
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

            task.setResources(new Resources());
            // 2020-04-29, soon-ish https://github.com/hashicorp/nomad/issues/646#issuecomment-596690053
            task.getResources().addNetworks(
                    new NetworkResource()
                            .addReservedPorts(new Port().setValue(port))
            );


            SubmissionToNomadJobAdapter
                    .getDockerNfsVolumesConfigurer(task)
                    .accept(new DockerNfsVolumes(List.of(new DockerNfsVolume(
                            "tensorboard-" + id + "-" + port,
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
                                    .setName("task-group-" + id)
                                    .setRestartPolicy(new RestartPolicy().setAttempts(0))
                                    .addTasks(task)
                    );

            try {
                nomad.getJobsApi().register(job);
                this.activeBoards.add(projectId);
                var publicUrl = this.routing.addRoute(
                        routePath,
                        new ProxyRouting.Route(
                                "http://" + publicIp + ":" + port + routeLocation,
                                u -> ProjectsController.canUserAccessProject(u, project)
                        )
                );
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return new ModelAndView("redirect:" + publicUrl + "/");

            } catch (IOException | NomadException e) {
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

    private synchronized int getNextPort() {
        return nextPort++;
    }

    @Nonnull
    private static String toNomadJobId(@Nonnull String projectId) {
        // TODO
        return projectId + "-tensorboard";
    }
}
