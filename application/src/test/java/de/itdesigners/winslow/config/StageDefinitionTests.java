package de.itdesigners.winslow.config;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.Environment;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class StageDefinitionTests {

    @Test
    public void testMostBasicPipeline() throws IOException {
        var stageYaml = """
                name: "The name of the stage"
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().getName().isEmpty());
        assertTrue(stage.getImage().getArgs().length == 0);
        assertTrue(stage.getRequirements().getCpu() == 0);
        assertTrue(stage.getRequirements().getGpu().getCount() == 0);
        assertTrue(stage.getEnvironment().isEmpty());
        assertTrue(stage.getHightlight().getResources().length == 0);
    }

    @Test
    public void testWithDescAndImage() throws IOException {

        var stageYaml = """
                    name: "The name of the stage"
                    description: "The description of the stage"
                    
                    image:
                        name: "image-origin/image-name"
                        args: ["arg1", "arg2"]
                """;


        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertEquals("The description of the stage", stage.getDescription());
        assertEquals("image-origin/image-name", stage.getImage().getName());
        assertArrayEquals(new String[]{"arg1", "arg2"}, stage.getImage().getArgs());
    }

    @Test
    public void testWithBasicRequires() throws IOException {

        var stageYaml = """
                    name: "The name of the stage"
                    
                    requirements:
                      cpu: 0
                      gpu:
                        count: 0
                        vendor: ""
                        support: []
                      megabytesOfRam: 4096
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().getName().isEmpty());
        assertEquals(4096, stage.getRequirements().getMegabytesOfRam());
    }

    @Test
    public void testWithGpuRequirements() throws IOException {


        var stageYaml = """
                   name: "The name of the stage" 
                    
                   requirements:
                     megabytesOfRam: 5120
                     gpu:
                       count: 4
                       vendor: "nvidia"
                       support: ["cuda", "vulkan"]
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().getName().isEmpty());
        assertEquals(5120, stage.getRequirements().getMegabytesOfRam());
        assertEquals(4, stage.getRequirements().getGpu().getCount());
        assertEquals("nvidia", stage.getRequirements().getGpu().getVendor());
        assertArrayEquals(new String[]{"cuda", "vulkan"}, stage.getRequirements().getGpu().getSupport());

    }

    @Test
    public void testWithEnvironmentVariables() throws IOException {

        var stageYaml = """
                name: "The name of the stage"
                                
                environment:
                    VAR_1: "VALUE_1"
                    VAR_2: "value_2"
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertEquals("VALUE_1", stage.getEnvironment().get("VAR_1"));
        assertEquals("value_2", stage.getEnvironment().get("VAR_2"));
    }

    @Test
    public void testWithHighlights() throws IOException {

        var stageYaml = """
                name: "The name of the stage"
                                
                highlight:
                    resources: ["res1", "RES/NUM/2"]
                """;

        var stage = BaseRepository.readFromString(StageDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().getName().isEmpty());
        assertTrue(stage.getEnvironment().isEmpty());
        assertArrayEquals(new String[]{"res1", "RES/NUM/2"}, stage.getHightlight().getResources());
    }

    @Test
    public void testSerialisationWithDefaultValues() throws IOException {

        var stage = new StageDefinition(
                null,
                "test",
                null,
                null,
                new Requirements(null, null, null),
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

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }

    @Test
    public void testSerialisationWithAllValues() throws IOException {

        var stage = new StageDefinition(
                UUID.randomUUID(),
                "test",
                "",
                new Image("image", new String[]{"param"}),
                new Requirements(4, 500, new Requirements.Gpu(4, "nvidia", new String[]{"test"})),
                new UserInput(UserInput.Confirmation.Always, Arrays.asList("Env")),
                Map.of("env1", "envValue"),
                new Highlight(new String[]{"to Highlight"}),
                true,
                true,
                Arrays.asList(new LogParser("matcher", "destination", "formatter", "type")),
                true,
                Arrays.asList("tag1", "tag2"),
                Map.of("result", "result_value"),
                null,
                Arrays.asList(UUID.randomUUID())
        );

        var stream = new ByteArrayOutputStream();
        BaseRepository.defaultWriter().store(stream, stage);

        var yaml = BaseRepository.writeToString(stage);

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }
}
