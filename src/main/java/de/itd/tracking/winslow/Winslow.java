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

        System.out.println(new Pipeline(pipe.getName(), pipe.getDescription().orElse(null), pipe.getUserInput().orElse(null), stages));


        System.out.println(pipeline.getTables("stage").get(0).toMap());


        /*
        try {
            Thread.sleep(100_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        var resourceManager = new ResourceManager(configuration.getPath(), new PathConfiguration());

        stages.stream().map(stage -> orchestrator.startOrNone(pipe, stage, new Environment(configuration, resourceManager))).flatMap(Optional::stream).forEach(stage -> {
            System.out.println("  ## >>>> RunningStage start >>> ##");
            for (String line : stage.getStdOut()) {
                System.out.print(line);
            }
            System.out.println("  ## <<<< RunningStage end <<<<< ##");
        });

    }
}
