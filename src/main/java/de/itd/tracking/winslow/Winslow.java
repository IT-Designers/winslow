package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;

import java.io.File;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private final Orchestrator               orchestrator;
    private final WorkDirectoryConfiguration configuration;
    private final ResourceManager            resourceManager;
    private final GroupRepository            groupRepository;
    private final UserRepository             userRepository;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration) {
        this.orchestrator = orchestrator;
        this.configuration = configuration;
        this.resourceManager = new ResourceManager(configuration.getPath(), new PathConfiguration());
        this.groupRepository = new GroupRepository();
        this.userRepository = new UserRepository(groupRepository);
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void run() {
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
}
