package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class StageWorkerDefinitionTests {

    @Test
    public void testMostBasicPipeline() throws IOException {
        var stageYaml = """
                Worker:
                    name: "The name of the stage"
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.image().getName().isEmpty());
        assertTrue(stage.image().getArgs().length == 0);
        assertTrue(stage.requirements().getCpus() == 0);
        assertTrue(stage.requirements().getGpu().getCount() == 0);
        assertTrue(stage.environment().isEmpty());
        assertTrue(stage.highlight().resources().size() == 0);

    }

    @Test
    public void testWithDescAndImage() throws IOException {

        var stageYaml = """
                    Worker:
                        name: "The name of the stage"
                        description: "The description of the stage"
                        
                        image:
                            name: "image-origin/image-name"
                            args: ["arg1", "arg2"]
                """;


        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertEquals("The description of the stage", stage.description());
        assertEquals("image-origin/image-name", stage.image().getName());
        assertArrayEquals(new String[]{"arg1", "arg2"}, stage.image().getArgs());
    }

    @Test
    public void testWithBasicRequires() throws IOException {

        var stageYaml = """
                    Worker:
                        name: "The name of the stage"
                        
                        requirements:
                          cpu: 0
                          gpu:
                            count: 0
                            vendor: ""
                            support: []
                          megabytesOfRam: 4096
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.image().getName().isEmpty());
        assertEquals(4096, stage.requirements().getMegabytesOfRam());
    }

    @Test
    public void testWithGpuRequirements() throws IOException {


        var stageYaml = """
                   Worker:
                       name: "The name of the stage" 
                        
                       requirements:
                         megabytesOfRam: 5120
                         gpu:
                           count: 4
                           vendor: "nvidia"
                           support: ["cuda", "vulkan"]
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.image().getName().isEmpty());
        assertEquals(5120, stage.requirements().getMegabytesOfRam());
        assertEquals(4, stage.requirements().getGpu().getCount());
        assertEquals("nvidia", stage.requirements().getGpu().getVendor());
        assertArrayEquals(new String[]{"cuda", "vulkan"}, stage.requirements().getGpu().getSupport());

    }

    @Test
    public void testWithEnvironmentVariables() throws IOException {

        var stageYaml = """
                Worker:
                    name: "The name of the stage"
                                    
                    environment:
                        VAR_1: "VALUE_1"
                        VAR_2: "value_2"
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertEquals("VALUE_1", stage.environment().get("VAR_1"));
        assertEquals("value_2", stage.environment().get("VAR_2"));
    }

    @Test
    public void testWithHighlights() throws IOException {

        var stageYaml = """
                Worker:
                    name: "The name of the stage"
                                    
                    highlight:
                        resources: ["res1", "RES/NUM/2"]
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.image().getName().isEmpty());
        assertTrue(stage.environment().isEmpty());

        var refList = List.of("res1", "RES/NUM/2");

        assertTrue(
                refList.containsAll(stage.highlight().resources()) &&
                        stage.highlight().resources().containsAll(refList));


    }

    @Test
    public void testSerialisationWithDefaultValues() throws IOException {

        var stage = new StageWorkerDefinition(
                null,
                "test",
                null,
                null,
                Requirements.createDefault(),
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

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }

    @Test
    public void testSerialisationWithAllValues() throws IOException {

        var stage = new StageWorkerDefinition(
                UUID.randomUUID(),
                "test",
                "",
                new Image("image", new String[]{"param"}),
                new Requirements(
                        4,
                        500,
                        new Requirements.Gpu(4, "nvidia", new String[]{"test"}),
                        Arrays.asList("tag1", "tag2")
                ),
                new UserInput(UserInput.Confirmation.Always, Arrays.asList("Env")),
                Map.of("env1", "envValue"),
                new Highlight(List.of("to Highlight")),
                true,
                true,
                Arrays.asList(new LogParser("matcher", "destination", "formatter", "type")),
                true,
                Arrays.asList(UUID.randomUUID())
        );

        var yaml = BaseRepository.writeToString(stage);

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }
}
