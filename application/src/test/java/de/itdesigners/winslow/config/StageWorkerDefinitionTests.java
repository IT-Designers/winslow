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
                    id: "b7c2867a-e82b-4195-9c1d-ce168a709bff"
                    name: "The name of the stage"
                    image:
                      name: "hello-world"
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("b7c2867a-e82b-4195-9c1d-ce168a709bff"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertEquals("hello-world", stage.image().getName());
        assertEquals(0, stage.image().getArgs().length);
        assertEquals(Optional.empty(), stage.requirements().getCpus());
        assertEquals(0, stage.requirements().getGpu().getCount());
        assertTrue(stage.environment().isEmpty());
        assertTrue(stage.highlight().resources().isEmpty());

    }

    @Test
    public void testWithDescAndImage() throws IOException {
        var stageYaml = """
                    Worker:
                        id: "2b6bbc37-c538-46c2-a139-2ecc65508f23"
                        name: "The name of the stage"
                        description: "The description of the stage"
                        image:
                            name: "image-origin/image-name"
                            args: ["arg1", "arg2"]
                """;


        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("2b6bbc37-c538-46c2-a139-2ecc65508f23"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertEquals("The description of the stage", stage.description());
        assertEquals("image-origin/image-name", stage.image().getName());
        assertArrayEquals(new String[]{"arg1", "arg2"}, stage.image().getArgs());
    }

    @Test
    public void testWithBasicRequires() throws IOException {
        var stageYaml = """
                    Worker:
                        id: "3fcaf5fa-c35b-4095-8714-a3266625eb3e"
                        name: "The name of the stage"
                        image:
                          name: "hello-world"
                        requirements:
                          cpus: 0
                          gpu:
                            count: 1
                            vendor: "nvidia"
                            support: []
                          megabytesOfRam: 4096
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("3fcaf5fa-c35b-4095-8714-a3266625eb3e"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertEquals("hello-world", stage.image().getName());
        assertEquals(1, stage.requirements().getGpu().getCount());
        assertEquals(Optional.of("nvidia"), stage.requirements().getGpu().getVendor());
        assertEquals(Optional.of(4096L), stage.requirements().getMegabytesOfRam());
    }

    @Test
    public void testWithGpuRequirements() throws IOException {
        var stageYaml = """
                   Worker:
                       id: "70a70ea0-f726-49d0-8863-a4a697bb67ff"
                       name: "The name of the stage"
                       image:
                         name: "hello-world"
                       requirements:
                         megabytesOfRam: 5120
                         gpu:
                           count: 4
                           vendor: "nvidia"
                           support: ["cuda", "vulkan"]
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("70a70ea0-f726-49d0-8863-a4a697bb67ff"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertEquals("hello-world", stage.image().getName());
        assertEquals(5120L, stage.requirements().getMegabytesOfRam().orElseThrow().longValue());
        assertEquals(4, stage.requirements().getGpu().getCount());
        assertEquals("nvidia", stage.requirements().getGpu().getVendor().orElse(null));
        assertArrayEquals(new String[]{"cuda", "vulkan"}, stage.requirements().getGpu().getSupport());

    }

    @Test
    public void testWithEnvironmentVariables() throws IOException {
        var stageYaml = """
                Worker:
                    id: "3c796e60-212a-4a6b-ba93-6f23be11ddd0"
                    name: "The name of the stage"
                    image:
                      name: "hello-world"
                    environment:
                        VAR_1: "VALUE_1"
                        VAR_2: "value_2"
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("3c796e60-212a-4a6b-ba93-6f23be11ddd0"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertEquals("VALUE_1", stage.environment().get("VAR_1"));
        assertEquals("value_2", stage.environment().get("VAR_2"));
    }

    @Test
    public void testWithHighlights() throws IOException {
        var stageYaml = """
                Worker:
                    id: "8f366e1e-2c1a-44cc-925c-b737dbf6ff8f"
                    name: "The name of the stage"
                    image:
                      name: "hello-world"
                    highlight:
                        resources: ["res1", "RES/NUM/2"]
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals(UUID.fromString("8f366e1e-2c1a-44cc-925c-b737dbf6ff8f"), stage.id());
        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.environment().isEmpty());

        var refList = List.of("res1", "RES/NUM/2");

        assertTrue(refList.containsAll(stage.highlight().resources()));
        assertTrue(stage.highlight().resources().containsAll(refList));


    }

    @Test
    public void testSerialisationWithDefaultValues() throws IOException {
        var stage = new StageWorkerDefinition(UUID.randomUUID(), "test", new Image("hello-world"));
        var yaml  = BaseRepository.writeToString(stage);

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }

    @Test
    public void testSerialisationWithAllValues() throws IOException {
        var stage = new StageWorkerDefinition(
                UUID.randomUUID(),
                "test",
                "some description",
                List.of(UUID.randomUUID()),
                new Image("image", new String[]{"param"}),
                new Requirements(
                        4,
                        500L,
                        new Requirements.Gpu(4, "nvidia", new String[]{"test"}),
                        Arrays.asList("tag1", "tag2")
                ),
                new UserInput(UserInput.Confirmation.ALWAYS, List.of("Env")),
                Map.of("env1", "envValue"),
                new Highlight(List.of("to Highlight")),
                List.of(new LogParser("matcher", "destination", "formatter", "type")),
                true,
                true,
                true
        );

        var yaml = BaseRepository.writeToString(stage);

        assertNotNull(yaml);
        assertNotEquals("", yaml);
    }
}
