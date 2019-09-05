package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;
import org.bouncycastle.cert.ocsp.OCSPException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private final Orchestrator               orchestrator;
    private final WorkDirectoryConfiguration configuration;
    private final LockBus                    lockBus;
    private final ResourceManager            resourceManager;
    private final GroupRepository            groupRepository;
    private final UserRepository             userRepository;
    private final PipelineRepository         pipelineRepository;
    private final ProjectRepository          projectRepository;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration) throws IOException {
        this.orchestrator = orchestrator;
        this.configuration = configuration;
        this.lockBus = new LockBus(configuration.getEventsDirectory());
        this.resourceManager = new ResourceManager(configuration.getPath(), new PathConfiguration());
        this.groupRepository = new GroupRepository();
        this.userRepository = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineRepository(lockBus, configuration);
        this.projectRepository = new ProjectRepository(lockBus, configuration);
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }

    public void run() {
        try {


            while (true) {
                getProjectRepository()
                        .getProjects()
                        .filter(handle -> {
                            var loaded = handle.unsafe();
                            return loaded.isPresent() && canMakeProgress(loaded.get());
                        })
                        .flatMap(handle -> handle.locked().stream())
                        .peek(this::tryMakeProgress)
                        .forEach(LockedContainer::close);

                synchronized (this) {
                    this.wait(10_000);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted unexpectedly", e);
        }
    }

    protected boolean canMakeProgress(@Nonnull Project project) {
        var running = orchestrator.getCurrentlyRunningStage(project);
        try {
            return running.isEmpty() || running.get().hasCompleted();
        } catch (OrchestratorConnectionException e) {
            e.printStackTrace(); // TODO
            return false;
        }
    }

    protected void tryMakeProgress(LockedContainer<Project> container) {
        try {
            var containerProject = container.get();
            if (containerProject.isPresent() && canMakeProgress(containerProject.get())) {
                var project = containerProject.get();
                orchestrator.startNextStage(project, new Environment(configuration, resourceManager));
                container.update(project);
            }
        } catch (LockException | IOException e) {
            e.printStackTrace();
        }
    }

    public void _run() {
        // vehicletracker_pipeline_draft_2.toml
        var pipeline = new Toml().read(new File("minimal.pipeline.toml"));
        pipeline.getTables("stage").stream().map(Toml::toMap).forEach(System.out::println);

        System.out.println(pipeline.getTable("pipeline").toMap());


        var pipe = pipeline.getTable("pipeline").to(Pipeline.class);
        System.out.println(pipe);

        var stages = pipeline.getTables("stage").stream().map(table -> table.to(Stage.class)).collect(Collectors.toList());

        pipe = new Pipeline(pipe.getName(), pipe.getDescription().orElse(null), pipe.getUserInput().orElse(null), stages);
        System.out.println(pipe);

        resourceManager.getResourceDirectory().ifPresent(dir -> {

        });


        var executor = new PipelineExecutor(pipe, orchestrator, new Environment(configuration, resourceManager));

        try {
            while (executor.poll().isEmpty()) {
                var stage = executor.getCurrentStage();
                if (stage.isPresent()) {
                    for (String string : stage.get().getStdOut()) {
                        System.out.print(string);
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (OrchestratorException e) {
            e.printStackTrace();
        }
    }

    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    public PipelineRepository getPipelineRepository() {
        return pipelineRepository;
    }
}
