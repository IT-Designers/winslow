package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

public class StageDefinitionTests {


    @Test
    public void WorkerDeserialization() throws IOException {
        var stageYaml = """
                Worker:
                    id: 4bfcf1f4-f92a-4e0b-93e8-61d06f0c971a
                    name: "The name of the stage"
                    image:
                      name: "hello-world"
                      args: []
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertTrue(stage instanceof StageWorkerDefinition);
        var worker = (StageWorkerDefinition) stage;
        assertEquals("The name of the stage", stage.name());
        assertTrue(worker.description().isEmpty());
        assertEquals("hello-world", worker.image().getName());
        assertEquals(0, worker.image().getArgs().length);
        assertEquals(Optional.empty(), worker.requirements().getCpus());
        assertEquals(0, worker.requirements().getGpu().getCount());
        assertTrue(worker.environment().isEmpty());
        assertTrue(worker.highlight().isEmpty());

    }


    @Test
    public void XorDeserialization() throws IOException {
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
    public void AndDeserialization() throws IOException {
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
        var stage = new StageWorkerDefinition(
                UUID.randomUUID(),
                "The name of the stage",
                (String) null,
                new Image("hello-world"),
                new Requirements(),
                new UserInput(),
                null,
                null,
                false,
                false,
                null,
                false,
                null
        );

        var yaml = BaseRepository.writeToString(stage);

        assertNotNull(yaml);
        assertNotEquals("", yaml);

    }


}
