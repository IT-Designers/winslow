package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class StageDefinitionTests {


    @Test
    public void WorkerDeserialistion() throws IOException {
        var stageYaml = """
                Worker:
                    name: "The name of the stage"
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertTrue(stage instanceof StageWorkerDefinition);
        var worker = (StageWorkerDefinition) stage;
        assertEquals("The name of the stage", stage.name());
        assertTrue(worker.description().isEmpty());
        assertTrue(worker.image().getName().isEmpty());
        assertTrue(worker.image().getArgs().length == 0);
        assertTrue(worker.requirements().getCpus() == 0);
        assertTrue(worker.requirements().getGpu().getCount() == 0);
        assertTrue(worker.environment().isEmpty());
        assertTrue(worker.highlight().resources().size() == 0);

    }


    @Test
    public void XorDeserialistion() throws IOException {
        var stageYaml = """
                XorGateway:
                    name: "The name of the stage"
                    conditions: ["A", "B"]
                    
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertTrue(stage instanceof StageXOrGatwayDefinition);
        var xor = (StageXOrGatwayDefinition) stage;
        assertEquals("The name of the stage", stage.name());
        assertTrue(xor.description().isEmpty());
        assertTrue(xor.conditions().size() == 2);

        assertTrue(xor.environment().isEmpty());
    }

    @Test
    public void AndDeserialistion() throws IOException {
        var stageYaml = """
                AndGateway:
                    name: "The name of the stage"
                    
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertTrue(stage instanceof StageAndGatewayDefinition);
        var xor = (StageAndGatewayDefinition) stage;
        assertEquals("The name of the stage", stage.name());
        assertTrue(xor.description().isEmpty());

        assertTrue(xor.environment().isEmpty());
    }


    @Test
    public void DefaultStageWorkerSerialisation() throws IOException {
        var stageYaml = """
                name: "The name of the stage"
                """;

        var stage = new StageWorkerDefinition(
                null,
                "The name of the stage",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var yaml = BaseRepository.writeToString(stage);
        System.out.println(yaml);

    }


}
