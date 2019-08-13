package de.itd.tracking.winslow.config;

import com.moandjiezana.toml.Toml;
import org.junit.Test;

import static org.junit.Assert.*;

public class StageTests {

    @Test
    public void testMostBasicPipeline() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\""
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().isEmpty());
        assertTrue(stage.getRequirements().isEmpty());
        assertTrue(stage.getEnvironment().isEmpty());
        assertTrue(stage.getHighlight().isEmpty());
    }

    @Test
    public void testWithDescAndImage() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\"\n" +
                "desc = \"The description of the stage\"" +
                "\n" +
                "[stage.image]\n" +
                "name = \"image-origin/image-name\"\n" +
                "args = [\"arg1\", \"arg2\"]"
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertEquals("The description of the stage", stage.getDescription().get());
        assertTrue(stage.getImage().isPresent());
        assertEquals("image-origin/image-name", stage.getImage().get().getName());
        assertArrayEquals(new String[]{"arg1", "arg2"}, stage.getImage().get().getArguments());
    }

    @Test
    public void testWithBasicRequires() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\"" +
                "\n" +
                "[stage.requires]\n" +
                "ram = 4096"
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().isEmpty());
        assertTrue(stage.getRequirements().isPresent());
        assertEquals(4096, stage.getRequirements().get().getMegabytesOfRam());
    }

    @Test
    public void testWithGpuRequirements() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\"" +
                "\n" +
                "[stage.requires]\n" +
                "ram = 5120\n" +
                "\n" +
                "[stage.requires.gpu]\n" +
                "count = 4\n" +
                "vendor = \"nvidia\"\n" +
                "support = [\"cuda\", \"vulkan\"]"
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().isEmpty());
        assertTrue(stage.getRequirements().isPresent());
        assertEquals(5120, stage.getRequirements().get().getMegabytesOfRam());
        assertTrue(stage.getRequirements().get().getGpu().isPresent());
        assertEquals(4, stage.getRequirements().get().getGpu().get().getCount());
        assertEquals("nvidia", stage.getRequirements().get().getGpu().get().getVendor().get());
        assertArrayEquals(new String[]{"cuda", "vulkan"}, stage.getRequirements().get().getGpu().get().getSupport());
    }

    @Test
    public void testWithEnvironmentVariables() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\"\n" +
                "\n" +
                "[stage.env]\n" +
                "VAR_1 = \"VALUE_1\"\n" +
                "VAR_2 = \"value_2\""
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().isEmpty());
        assertTrue(stage.getRequirements().isEmpty());
        assertEquals("VALUE_1", stage.getEnvironment().get("VAR_1"));
        assertEquals("value_2", stage.getEnvironment().get("VAR_2"));
    }

    @Test
    public void testWithHighlights() {
        var stage = new Toml().read("[stage]\n" +
                "name = \"The name of the stage\"\n" +
                "\n" +
                "[stage.highlight]\n" +
                "resources = [\"res1\", \"RES/NUM/2\"]"
        )
                .getTable("stage")
                .to(Stage.class);

        assertEquals("The name of the stage", stage.getName());
        assertTrue(stage.getDescription().isEmpty());
        assertTrue(stage.getImage().isEmpty());
        assertTrue(stage.getRequirements().isEmpty());
        assertTrue(stage.getEnvironment().isEmpty());
        assertTrue(stage.getHighlight().isPresent());
        assertArrayEquals(new String[]{"res1", "RES/NUM/2"}, stage.getHighlight().get().getResources());
    }
}
