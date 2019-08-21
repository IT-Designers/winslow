package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import com.hashicorp.nomad.javasdk.NomadException;
import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private final Orchestrator               orchestrator;
    private final WorkDirectoryConfiguration configuration;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration) {
        this.orchestrator = orchestrator;
        this.configuration = configuration;
    }


    public void run() {
        System.out.println("running");
        var config = new NomadApiConfiguration.Builder().setAddress("http://localhost:4646").build();

        var client = new NomadApiClient(config);

        try {
            System.out.println("Listing jobs");
            client.getJobsApi().list().getValue().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        }

        // vehicletracker_pipeline_draft_2.toml
        var pipeline = new Toml().read(new File("minimal.pipeline.toml"));
        pipeline.getTables("stage").stream().map(Toml::toMap).forEach(System.out::println);

        System.out.println(pipeline.getTable("pipeline").toMap());


        var pipe = pipeline.getTable("pipeline").to(Pipeline.class);
        System.out.println(pipe);

        var stages = pipeline.getTables("stage").stream().map(table -> table.to(Stage.class)).collect(Collectors.toList());

        pipe = new Pipeline(pipe.getName(), pipe.getDescription().orElse(null), pipe.getUserInput().orElse(null), stages);
        System.out.println(pipe);

        var resourceManager = new ResourceManager(configuration.getPath(), new PathConfiguration());

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
}
