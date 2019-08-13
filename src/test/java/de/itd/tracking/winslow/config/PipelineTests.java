package de.itd.tracking.winslow.config;

import com.moandjiezana.toml.Toml;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class PipelineTests {

    @Test
    public void testMostBasicPipeline() {
        var pipeline = new Toml().read("[pipeline]\n" +
                "name = \"Name of the pipeline\"\n" +
                "desc = \"Description of the pipeline\"\n"
        )
                .getTable("pipeline")
                .to(Pipeline.class);

        assertEquals("Name of the pipeline", pipeline.getName());
        assertEquals("Description of the pipeline", pipeline.getDescription().get());
        assertTrue(pipeline.getUserInput().isEmpty());
        assertTrue(pipeline.getStages().isEmpty());
    }

    @Test
    public void testPipelineWithUserInputForVal() {
        var pipeline = new Toml().read("[pipeline.userInput]\n" +
                "valueFor = [\"KEY_A\", \"KEY_B\"]\n"
        )
                .getTable("pipeline")
                .to(Pipeline.class);

        assertNull(pipeline.getName());
        assertTrue(pipeline.getDescription().isEmpty());
        assertTrue(pipeline.getUserInput().isPresent());
        assertEquals(Arrays.asList("KEY_A", "KEY_B"), pipeline.getUserInput().get().getValueFor());
        assertTrue(pipeline.getStages().isEmpty());
    }
}
