package de.itdesigners.winslow.config;
import de.itdesigners.winslow.BaseRepository;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StageDefinitionTests {


    @Test
    public void testMostBasicPipeline() throws IOException {
        var stageYaml = """
                name: "The name of the stage"
                """;

        var stage = BaseRepository.readFromString(StageWorkerDefinition.class, stageYaml);

        assertEquals("The name of the stage", stage.name());
        assertTrue(stage.description().isEmpty());
        assertTrue(stage.image().getName().isEmpty());
        assertTrue(stage.image().getArgs().length == 0);
        assertTrue(stage.requirements().getCpu() == 0);
        assertTrue(stage.requirements().getGpu().getCount() == 0);
        assertTrue(stage.environment().isEmpty());
        assertTrue(stage.highlight().resources().size() == 0);

    }

}
