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

        assertEquals(
                graph.getNodes().get(0).getNextNodes(),
                List.of(graph.getNodes().get(1))
        );
        assertEquals(
                graph.getNodes().get(1).getPreviousNodes(),
                List.of(graph.getNodes().get(0))
        );

        assertEquals(
                graph.getNodes().get(1).getNextNodes(),
                List.of(graph.getNodes().get(2))
        );
        assertEquals(
                graph.getNodes().get(2).getPreviousNodes(),
                List.of(graph.getNodes().get(1))
        );
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

        assertEquals(
                graph.getNodes().get(0).getNextNodes(),
                List.of(graph.getNodes().get(2))
        );
        assertEquals(
                graph.getNodes().get(1).getNextNodes(),
                List.of(graph.getNodes().get(2))
        );

        assertEquals(
                graph.getNodes().get(2).getPreviousNodes(),
                List.of(graph.getNodes().get(0), graph.getNodes().get(1))
        );
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
