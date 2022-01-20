package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                        newStupidStageDefinition("stage-1", List.of("stage-3")),
                        newStupidStageDefinition("stage-2", List.of("stage-3")),
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

    @Test
    public void testComplexGraphWithoutExecutionGroups() {
        var pipelineDefinition = getComplexPipelineDefinition();
        var graph = new Graph(
                emptyPipeline(),
                pipelineDefinition,
                new Node(
                        pipelineDefinition.getStages().get(0),
                        null
                )
        );

        testComplexGraphByDefinitions(graph);
    }

    private static void testComplexGraphByDefinitions(@Nonnull Graph graph) {
        var nodeA = graph.getNodeForStageDefinitionName("def-a").orElseThrow();
        var nodeB = graph.getNodeForStageDefinitionName("def-b").orElseThrow();
        var nodeC = graph.getNodeForStageDefinitionName("def-c").orElseThrow();
        var nodeD = graph.getNodeForStageDefinitionName("def-d").orElseThrow();
        var nodeE = graph.getNodeForStageDefinitionName("def-e").orElseThrow();
        var nodeF = graph.getNodeForStageDefinitionName("def-f").orElseThrow();
        var nodeG = graph.getNodeForStageDefinitionName("def-g").orElseThrow();
        var node1 = graph.getNodeForStageDefinitionName("gtw-1").orElseThrow();
        var node2 = graph.getNodeForStageDefinitionName("gtw-2").orElseThrow();
        var node3 = graph.getNodeForStageDefinitionName("gtw-3").orElseThrow();

        assertContainsAll(
                List.of(node1),
                nodeA.getNextNodes()
        );

        assertContainsAll(
                List.of(nodeB, nodeC),
                node1.getNextNodes()
        );

        assertContainsAll(
                List.of(node3),
                nodeB.getNextNodes()
        );

        assertContainsAll(
                List.of(nodeB,nodeC),
                node1.getNextNodes()
        );

        assertContainsAll(
                List.of(nodeD, nodeE),
                node2.getNextNodes()
        );

        assertContainsAll(
                List.of(nodeF),
                nodeD.getNextNodes()
        );

        assertContainsAll(
                List.of(node3),
                nodeE.getNextNodes()
        );

        assertContainsAll(
                List.of(node3),
                nodeF.getNextNodes()
        );

        assertContainsAll(
                List.of(nodeG),
                node3.getNextNodes()
        );

        assertContainsAll(
                List.of(),
                nodeG.getNextNodes()
        );

        assertTrue(nodeG.getPreviousNodes().contains(node3));
        assertEquals(1, nodeG.getPreviousNodes().size());

        assertTrue(node3.getPreviousNodes().contains(nodeB));
        assertTrue(node3.getPreviousNodes().contains(nodeE));
        assertTrue(node3.getPreviousNodes().contains(nodeF));
        assertEquals(3, node3.getPreviousNodes().size());

        assertTrue(nodeF.getPreviousNodes().contains(nodeD));
        assertEquals(1, nodeF.getPreviousNodes().size());

        assertTrue(nodeE.getPreviousNodes().contains(node2));
        assertEquals(1, nodeE.getPreviousNodes().size());

        assertTrue(nodeD.getPreviousNodes().contains(node2));
        assertEquals(1, nodeD.getPreviousNodes().size());

        assertTrue(node2.getPreviousNodes().contains(nodeC));
        assertEquals(1, node2.getPreviousNodes().size());

        assertTrue(nodeC.getPreviousNodes().contains(node1));
        assertEquals(1, nodeC.getPreviousNodes().size());

        assertTrue(node1.getPreviousNodes().contains(nodeA));
        assertEquals(1, node1.getPreviousNodes().size());

        assertTrue(nodeA.getPreviousNodes().isEmpty());
        assertEquals(0, nodeA.getPreviousNodes().size());
    }

    private static <T> void assertContainsAll(@Nonnull List<T> should, @Nonnull List<T> actual) {
        var list = new ArrayList<>(should);
        for (T p : actual) {
            assertTrue("List should not contain: " + p, list.remove(p));
        }
        assertTrue("List should be empty but is not: " + list, list.isEmpty());
    }

    @Test
    public void testComplexGraphWithSomeExecutionGroupsAndGateways() {
        var pipelineDefinition = getComplexPipelineDefinition();

        var defA = getStageDefinition(pipelineDefinition, "def-a");
        var defB = getStageDefinition(pipelineDefinition, "def-b");
        var defC = getStageDefinition(pipelineDefinition, "def-c");
        var defD = getStageDefinition(pipelineDefinition, "def-d");
        var defE = getStageDefinition(pipelineDefinition, "def-e");
        var defF = getStageDefinition(pipelineDefinition, "def-f");
        var defG = getStageDefinition(pipelineDefinition, "def-g");
        var gtw1 = getStageDefinition(pipelineDefinition, "gtw-1");
        var gtw2 = getStageDefinition(pipelineDefinition, "gtw-2");
        var gtw3 = getStageDefinition(pipelineDefinition, "gtw-3");

        var exgA = emptyExecutionGroup(defA, null);
        var exg1 = emptyExecutionGroup(gtw1, exgA.getId());
        var exgB = emptyExecutionGroup(defB, exg1.getId());
        var exgC = emptyExecutionGroup(defC, exg1.getId());
        var exg2 = emptyExecutionGroup(gtw2, exgC.getId());
        var exgD = emptyExecutionGroup(defD, exg2.getId());
        var exgE = emptyExecutionGroup(defE, exg2.getId());
        var exgF = emptyExecutionGroup(defF, exgD.getId());
        var exg3E = emptyExecutionGroup(gtw3, exgE.getId());
        var exg3F = emptyExecutionGroup(gtw3, exgF.getId());
        var exg3B = emptyExecutionGroup(gtw3, exgB.getId());
        var exgG = emptyExecutionGroup(defG, exg3F.getId());

        var graph = new Graph(
                simplePipeline(
                        List.of(exgA, exg1, exgB, exgC, exg2, exgD, exgE, exgF, exg3E, exg3F, exg3B, exgG),
                        List.of(),
                        List.of()
                ),
                pipelineDefinition,
                new Node(defG, exgG)
        );

        testComplexGraphByDefinitions(graph);

        assertEquals(List.of(exgA), graph.getNodeForStageDefinitionName("def-a").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg1), graph.getNodeForStageDefinitionName("gtw-1").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgB), graph.getNodeForStageDefinitionName("def-b").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgC), graph.getNodeForStageDefinitionName("def-c").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg2), graph.getNodeForStageDefinitionName("gtw-2").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgD), graph.getNodeForStageDefinitionName("def-d").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgE), graph.getNodeForStageDefinitionName("def-e").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgF), graph.getNodeForStageDefinitionName("def-f").orElseThrow().getExecutionGroups());

        assertContainsAll(List.of(exg3E, exg3F, exg3B), graph.getNodeForStageDefinitionName("gtw-3").orElseThrow().getExecutionGroups());

        assertEquals(List.of(exgG), graph.getNodeForStageDefinitionName("def-g").orElseThrow().getExecutionGroups());
    }

    @Test
    public void testComplexGraphWithSomeExecutionGroupsIgnorePreviousRuns() {
        var pipelineDefinition = getComplexPipelineDefinition();

        var defA = getStageDefinition(pipelineDefinition, "def-a");
        var defB = getStageDefinition(pipelineDefinition, "def-b");
        var defC = getStageDefinition(pipelineDefinition, "def-c");
        var defD = getStageDefinition(pipelineDefinition, "def-d");
        var defE = getStageDefinition(pipelineDefinition, "def-e");
        var defF = getStageDefinition(pipelineDefinition, "def-f");
        var defG = getStageDefinition(pipelineDefinition, "def-g");
        var gtw1 = getStageDefinition(pipelineDefinition, "gtw-1");
        var gtw2 = getStageDefinition(pipelineDefinition, "gtw-2");
        var gtw3 = getStageDefinition(pipelineDefinition, "gtw-3");


        var history = new ArrayList<ExecutionGroup>();

        for (int i = 0; i < 10; ++i) {
            var exgA  = emptyExecutionGroup(defA, null);
            var exg1  = emptyExecutionGroup(gtw1, exgA.getId());
            var exgB  = emptyExecutionGroup(defB, exg1.getId());
            var exgC  = emptyExecutionGroup(defC, exg1.getId());
            var exg2  = emptyExecutionGroup(gtw2, exgC.getId());
            var exgD  = emptyExecutionGroup(defD, exg2.getId());
            var exgE  = emptyExecutionGroup(defE, exg2.getId());
            var exgF  = emptyExecutionGroup(defF, exgD.getId());
            var exg3E = emptyExecutionGroup(gtw3, exgE.getId());
            var exg3F = emptyExecutionGroup(gtw3, exgF.getId());
            var exg3B = emptyExecutionGroup(gtw3, exgB.getId());
            var exgG  = emptyExecutionGroup(defG, exg3F.getId());
            history.addAll(List.of(exgA, exg1, exgB, exgC, exg2, exgD, exgE, exgF, exg3E, exg3F, exg3B, exgG));
        }

        var exgA = emptyExecutionGroup(defA, null);
        var exg1 = emptyExecutionGroup(gtw1, exgA.getId());
        // var exgB = emptyExecutionGroup(defB, exg1.getId()); // pretend it did not run
        var exgC = emptyExecutionGroup(defC, exg1.getId());
        var exg2 = emptyExecutionGroup(gtw2, exgC.getId());
        var exgD = emptyExecutionGroup(defD, exg2.getId());
        var exgE = emptyExecutionGroup(defE, exg2.getId());
        var exgF = emptyExecutionGroup(defF, exgD.getId());
        var exg3E = emptyExecutionGroup(gtw3, exgE.getId());
        var exg3F = emptyExecutionGroup(gtw3, exgF.getId());
        // var exg3B = emptyExecutionGroup(gtw3, exgB.getId());  // pretend it did not run
        // var exgG = emptyExecutionGroup(defG, exg3F.getId());  // pretend it did not run
        history.addAll(List.of(exgA, exg1, exgC, exg2, exgD, exgE, exgF, exg3E, exg3F));

        var graph = new Graph(
                simplePipeline(
                        history,
                        List.of(),
                        List.of()
                ),
                pipelineDefinition,
                new Node(gtw3, exg3F)
        );


        testComplexGraphByDefinitions(graph);

        assertEquals(List.of(exgA), graph.getNodeForStageDefinitionName("def-a").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg1), graph.getNodeForStageDefinitionName("gtw-1").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgC), graph.getNodeForStageDefinitionName("def-c").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg2), graph.getNodeForStageDefinitionName("gtw-2").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgD), graph.getNodeForStageDefinitionName("def-d").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgE), graph.getNodeForStageDefinitionName("def-e").orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgF), graph.getNodeForStageDefinitionName("def-f").orElseThrow().getExecutionGroups());

        assertContainsAll(List.of(exg3E, exg3F), graph.getNodeForStageDefinitionName("gtw-3").orElseThrow().getExecutionGroups());
    }

    @Test
    public void testComplexGraphWithSomeExecutionGroups1() {
        var pipelineDefinition = getComplexPipelineDefinition();

        var defA = getStageDefinition(pipelineDefinition, "def-a");
        var defB = getStageDefinition(pipelineDefinition, "def-b");
        var defC = getStageDefinition(pipelineDefinition, "def-c");
        var gtw1 = getStageDefinition(pipelineDefinition, "gtw-1");

        var exgA = emptyExecutionGroup(defA, null);
        var exg1 = emptyExecutionGroup(gtw1, exgA.getId());
        var exgC = emptyExecutionGroup(defC, exg1.getId());

        var graph = new Graph(
                simplePipeline(
                        List.of(exgA, exg1, exgC),
                        List.of(),
                        List.of()
                ),
                pipelineDefinition,
                new Node(defC, exgC)
        );

        // gtw1 & EG
        var nodeGtw1 = graph.getNodeForStageDefinitionName(gtw1.getName()).orElseThrow();
        assertEquals(1, nodeGtw1.getExecutionGroups().size());
        assertEquals(exg1, nodeGtw1.getExecutionGroups().get(0));
        assertEquals(2, nodeGtw1.getNextNodes().size());


        // gtw1.prev = A?
        assertEquals(1, nodeGtw1.getPreviousNodes().size());
        var nodeDefA = nodeGtw1.getPreviousNodes().get(0);

        // A.next = gtw1
        assertEquals(List.of(nodeGtw1), nodeDefA.getNextNodes());

        // A.EG = exgA?
        assertEquals(defA, nodeDefA.getStageDefinition());
        assertEquals(List.of(exgA), nodeDefA.getExecutionGroups());


        // gtw1.next = C
        var nodeDefC = nodeGtw1
                .getNextNodes()
                .stream()
                .filter(n -> n.getStageDefinition().equals(defC))
                .findFirst()
                .orElseThrow();

        // C.prev = gtw1
        assertEquals(List.of(nodeGtw1), nodeDefC.getPreviousNodes());

        // C.EG = exgC?
        assertEquals(defC, nodeDefC.getStageDefinition());
        assertEquals(List.of(exgC), nodeDefC.getExecutionGroups());


        // gtw1.next = B
        var nodeDefB = nodeGtw1
                .getNextNodes()
                .stream()
                .filter(n -> n.getStageDefinition().equals(defB))
                .findFirst()
                .orElseThrow();

        // B.prev = gtw1
        assertEquals(List.of(nodeGtw1), nodeDefB.getPreviousNodes());

        // C.EG = <empty>?
        assertEquals(List.of(), nodeDefB.getExecutionGroups());
    }

    @Nonnull
    public static StageDefinition getStageDefinition(@Nonnull PipelineDefinition definition, @Nonnull String name) {
        return definition.getStages().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
    }

    @Nonnull
    public static PipelineDefinition getComplexPipelineDefinition() {
        /*
         *    [A] ---> (1) ---> [C] ---> (2) ------------> [E] ---> (3) ---> [G]
         *              |                 |                          ^
         *              |                 +-----> [D] ---> [F] ------+
         *              |                                            |
         *              + -------------------------------  [B] ------+
         *
         *   Ein Testszenario könnte sein:
         *      - ob (3) [E], [F] und [B] als Eingänge hat und EGs richtig zugeordnet sind
         *      - ob (1) [B] und [C] als Ausgänge hat
         *      - ob für Xor/And benötigte (Vor-)Bedingungen richtig zugeordnet werden
         */
        return newStupidPipelineDefinition("complex-pipeline", List.of(
                newStupidStageDefinition("def-a", List.of("gtw-1")),
                newStupidStageDefinition("gtw-1", List.of("def-b", "def-c")),

                // path gtw-1 upper / path def-c
                newStupidStageDefinition("def-c", List.of("gtw-2")),

                // path gtw-2 upper
                newStupidStageDefinition("gtw-2", List.of("def-d", "def-e")),
                newStupidStageDefinition("def-e", List.of("gtw-3")),

                // path gtw-2 lower
                newStupidStageDefinition("def-d", List.of("def-f")),
                newStupidStageDefinition("def-f", List.of("gtw-3")),

                // path gtw-1 lower
                newStupidStageDefinition("def-b", List.of("gtw-3")),


                newStupidStageDefinition("gtw-3", List.of("def-g")),
                newStupidStageDefinition("def-g", List.of())
        ));
    }

    @Nonnull
    public static PipelineDefinition getDirectPipelineDefinition(@Nonnull String... stageNames) {
        var stages = new ArrayList<StageDefinition>(stageNames.length);
        for (int i = 0; i < stageNames.length; ++i) {
            stages.add(newStupidStageDefinition(
                    stageNames[i],
                    i + 1 < stageNames.length ? List.of(stageNames[i + 1]) : null
            ));
        }

        return newStupidPipelineDefinition("the-name", stages);
    }

    @Nonnull
    public static PipelineDefinition newStupidPipelineDefinition(
            @Nonnull String name,
            @Nonnull List<StageDefinition> stages) {
        return new PipelineDefinition(name, null, null, stages, null, null, null);
    }

    @Nonnull
    public static StageDefinition newStupidStageDefinition(@Nonnull String name, @Nullable List<String> next) {
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
    public static ExecutionGroup emptyExecutionGroup(
            @Nonnull StageDefinition stageDefinition,
            @Nullable ExecutionGroupId parentId) {
        return new ExecutionGroup(
                new ExecutionGroupId("<project-id>", 0, UUID.randomUUID().toString()),
                stageDefinition,
                new WorkspaceConfiguration(),
                null,
                parentId
        );
    }

    @Nonnull
    public static Pipeline emptyPipeline() {
        return new Pipeline("<great-project-id-totally-valid-UUID-and-unsuspicious>");
    }

    @Nonnull
    public static Pipeline simplePipeline(
            @Nullable List<ExecutionGroup> history,
            @Nullable List<ExecutionGroup> active,
            @Nullable List<ExecutionGroup> queue
    ) {
        return new Pipeline(
                "<great-project-id-totally-valid-UUID-and-unsuspicious>",
                history,
                queue,
                active,
                false,
                null,
                null,
                null,
                null,
                1337
        );
    }
}
