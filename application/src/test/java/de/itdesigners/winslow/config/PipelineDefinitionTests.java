package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class PipelineDefinitionTests {

    @Test
    public void testMostBasicPipeline() throws IOException {

        var pipelineYaml = """ 
                name: "Name of the pipeline"
                description: "Description of the pipeline" 
                 """;


        var pipeline = BaseRepository.readFromString(PipelineDefinition.class, pipelineYaml);

        assertEquals("Name of the pipeline", pipeline.name());
        assertEquals("Description of the pipeline", pipeline.description());
        assertTrue(pipeline.userInput().getEnvironment().isEmpty());
        assertTrue(pipeline.stages().isEmpty());
    }

    @Test
    public void testPipelineWithUserInputForVal() throws IOException {


        var pipelineYaml = """ 
                name: "Test"
                userInput:
                    environment: ["KEY_A", "KEY_B"]
                 """;

        var pipeline = BaseRepository.readFromString(PipelineDefinition.class, pipelineYaml);

        assertNotNull(pipeline.name());
        assertTrue(pipeline.description().isEmpty());
        assertEquals(Arrays.asList("KEY_A", "KEY_B"), pipeline.userInput().getEnvironment());
        assertTrue(pipeline.stages().isEmpty());
    }


    @Test
    public void testDefaultSerialisation() throws IOException {


        var pipeline = new PipelineDefinition(
                "Pipeline",
                null,
                null,
                null,
                null,
                null,
                null
        );

        var yaml = BaseRepository.writeToString(pipeline);

        assertNotNull(yaml);
        assertNotEquals("", yaml);

    }


    @Test
    public void testSerialisationWithAllValues() throws IOException {


        var pipeline = new PipelineDefinition(
                "Pipeline",
                "description",
                new UserInput(UserInput.Confirmation.Always, Arrays.asList("env")),
                Arrays.asList(new StageWorkerDefinition(
                        null,
                        "pipeline",
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
                )),
                Map.of("env1", "envValue"),
                new DeletionPolicy(true, 10, true),
                Arrays.asList("markers")
        );

        var yaml = BaseRepository.writeToString(pipeline);

        assertNotNull(yaml);
        assertNotEquals("", yaml);

    }
}
