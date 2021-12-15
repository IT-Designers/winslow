package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Pipeline;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphTests {

    @Test
    public void simpleParentFollowTest() {
        var pipelineDefinition = getDirectPipelineDefinition("stage-1", "stage-2", "stage-3");

        var graph = new Graph(
                emptyPipeline(),
                pipelineDefinition,
                new Node(
                    pipelineDefinition.getStages().get(0),
                    null
                )
        );

        var node1 = graph.getNodeForStageDefinitionName("stage-1").orElseThrow();
        var node2 = graph.getNodeForStageDefinitionName("stage-2").orElseThrow();
        var node3 = graph.getNodeForStageDefinitionName("stage-3").orElseThrow();

        assertEquals(3, graph.getNodes().size());

        assertEquals(node1.getNextNodes(), List.of(node2));
        assertEquals(node2.getPreviousNodes(), List.of(node1));

        assertEquals(node2.getNextNodes(), List.of(node3));
        assertEquals(node3.getPreviousNodes(), List.of(node2));
    }

    @Test
    public void testSimpleJoinOf1And2Into3() {
        var pipelineDefinition = newStupidPipelineDefinition(
                "<JoinOf1And2Into3>",
                List.of(
                        newStupidStageDefinition("stage-1", "stage-3"),
                        newStupidStageDefinition("stage-2", "stage-3"),
                        newStupidStageDefinition("stage-3", null)
                )
        );

        var graph = new Graph(
                emptyPipeline(),
                pipelineDefinition,
                new Node(
                    pipelineDefinition.getStages().get(0),
                    null
                )
        );

        var node1 = graph.getNodeForStageDefinitionName("stage-1").orElseThrow();
        var node2 = graph.getNodeForStageDefinitionName("stage-2").orElseThrow();
        var node3 = graph.getNodeForStageDefinitionName("stage-3").orElseThrow();

        assertEquals(
                node1.getNextNodes(),
                List.of(node3)
        );
        assertEquals(
                node2.getNextNodes(),
                List.of(node3)
        );

        assertTrue(node3.getPreviousNodes().contains(node1));
        assertTrue(node3.getPreviousNodes().contains(node2));
        assertEquals(2, node3.getPreviousNodes().size());
        assertEquals(3, graph.getNodes().size());
    }

    @Nonnull
    public static PipelineDefinition getDirectPipelineDefinition(@Nonnull String... stageNames) {
        var stages = new ArrayList<StageDefinition>(stageNames.length);
        for (int i = 0; i < stageNames.length; ++i) {
            stages.add(newStupidStageDefinition(stageNames[i], i + 1 < stageNames.length ? stageNames[i + 1] : null));
        }

        return newStupidPipelineDefinition("the-name", stages);
    }

    @Nonnull
    public static PipelineDefinition newStupidPipelineDefinition(@Nonnull String name, @Nonnull List<StageDefinition> stages) {
        return new PipelineDefinition(name, null, null, stages, null, null, null);
    }

    @Nonnull
    public static StageDefinition newStupidStageDefinition(@Nonnull String name, @Nullable String next) {
        return new StageDefinition(
                name,
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
                null,
                null,
                null,
                next
        );
    }

    @Nonnull
    public static Pipeline emptyPipeline() {
        return new Pipeline("<great-project-id-totally-valid-UUID-and-unsuspicious>");
    }
}
