package de.itdesigners.winslow.config;

import com.moandjiezana.toml.Toml;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class PipelineDefinitionTests {

    @Test
    public void testMostBasicPipeline() {
        var pipeline = new Toml().read("[pipeline]\n" +
                "name = \"Name of the pipeline\"\n" +
                "desc = \"Description of the pipeline\"\n"
        )
                .getTable("pipeline")
                .to(PipelineDefinition.class);

        assertEquals("Name of the pipeline", pipeline.getName());
        assertEquals("Description of the pipeline", pipeline.getDescription().get());
        assertTrue(pipeline.getRequires().isEmpty());
        assertTrue(pipeline.getStages().isEmpty());
    }

    @Test
    public void testPipelineWithUserInputForVal() {
        var pipeline = new Toml().read("[pipeline.userInput]\n" +
                "valueFor = [\"KEY_A\", \"KEY_B\"]\n"
        )
                .getTable("pipeline")
                .to(PipelineDefinition.class);

        assertNull(pipeline.getName());
        assertTrue(pipeline.getDescription().isEmpty());
        assertTrue(pipeline.getRequires().isPresent());
        assertEquals(Arrays.asList("KEY_A", "KEY_B"), pipeline.getRequires().get().getEnvironment());
        assertTrue(pipeline.getStages().isEmpty());
    }
}
