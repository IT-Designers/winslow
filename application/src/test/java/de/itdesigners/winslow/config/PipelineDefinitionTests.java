package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class PipelineDefinitionTests {

    @Test
    public void testMostBasicPipeline() throws IOException {

        var pipelineYaml = """ 
                id: "this-should-be-a-uuid"
                name: "Name of the pipeline"
                description: "Description of the pipeline"
                 """;


        var pipeline = BaseRepository.readFromString(PipelineDefinition.class, pipelineYaml);

        assertEquals("Name of the pipeline", pipeline.name());
        assertEquals("Description of the pipeline", pipeline.description());
        assertTrue(pipeline.userInput().getRequiredEnvVariables().isEmpty());
        assertTrue(pipeline.stages().isEmpty());
    }

    @Test
    public void testPipelineWithUserInputForVal() throws IOException {


        var pipelineYaml = """ 
                id: "this-should-be-a-uuid"
                name: "Test"
                userInput:
                    requiredEnvVariables: ["KEY_A", "KEY_B"]
                 """;

        var pipeline = BaseRepository.readFromString(PipelineDefinition.class, pipelineYaml);

        assertNotNull(pipeline.name());
        assertTrue(pipeline.optDescription().isEmpty());
        assertEquals(Arrays.asList("KEY_A", "KEY_B"), pipeline.userInput().getRequiredEnvVariables());
        assertTrue(pipeline.stages().isEmpty());
    }


    @Test
    public void testDefaultSerialisation() throws IOException {
        var pipeline = new PipelineDefinition(UUID.randomUUID().toString(), "Pipeline");
        var yaml     = BaseRepository.writeToString(pipeline);

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }


    @Test
    public void testSerialisationWithAllValues() throws IOException {
        var pipeline = new PipelineDefinition(
                UUID.randomUUID().toString(),
                "Pipeline",
                "description",
                new UserInput(UserInput.Confirmation.ALWAYS, List.of("env")),
                List.of(new StageWorkerDefinition(
                        UUID.randomUUID(),
                        "pipeline",
                        new Image("hello-world")
                )),
                Map.of("env1", "envValue"),
                new DeletionPolicy(true, true, 10),
                List.of("markers"),
                List.of(
                        new Link("OwnerBaer", Role.OWNER),
                        new Link("MemberBaer", Role.MEMBER)
                ),
                true
        );

        var yaml = BaseRepository.writeToString(pipeline);

        assertNotNull(yaml);
        assertNotEquals("", yaml);

        var result = BaseRepository.readFromString(PipelineDefinition.class, yaml);
        assertEquals(pipeline, result);
    }
}
