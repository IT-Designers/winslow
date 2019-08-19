package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import com.hashicorp.nomad.javasdk.NomadException;
import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private final Orchestrator orchestrator;

    public Winslow(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
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

        var ids = stages.stream().map(stage -> orchestrator.start(pipe, stage, new Environment())).collect(Collectors.toList());

        for (int i = 0; i < 3; ++i) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (String id : ids) {
                try {
                    System.out.println(client.getJobsApi().info(id).getValue());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NomadException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            for (var job : client.getJobsApi().list().getValue()) {
                //client.getJobsApi().deregister(job.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NomadException e) {
            e.printStackTrace();
        }
    }
}
