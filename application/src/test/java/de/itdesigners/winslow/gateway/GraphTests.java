package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;
import org.javatuples.Pair;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphTests {

    // some uuids for some stage definitions
    static UUID DEF_A = UUID.randomUUID();
    static UUID DEF_B = UUID.randomUUID();
    static UUID DEF_C = UUID.randomUUID();
    static UUID DEF_D = UUID.randomUUID();
    static UUID DEF_E = UUID.randomUUID();
    static UUID DEF_F = UUID.randomUUID();
    static UUID DEF_G = UUID.randomUUID();
    static UUID GTW_1 = UUID.randomUUID();
    static UUID GTW_2 = UUID.randomUUID();
    static UUID GTW_3  = UUID.randomUUID();


    @Test
    public void simpleParentFollowTest() {

        var pair = getDirectPipelineDefinition("stage-1", "stage-2", "stage-3");

        var pipelineDefinition = pair.getValue0();
        var uuids = pair.getValue1();

        var graph = new Graph(
                emptyPipeline(),
                pipelineDefinition,
                new Node(
                        pipelineDefinition.stages().get(0),
                        null
                )
        );

        var node1 = graph.getNodeForStageDefinitionId(uuids.get(0)).orElseThrow();
        var node2 = graph.getNodeForStageDefinitionId(uuids.get(1)).orElseThrow();
        var node3 = graph.getNodeForStageDefinitionId(uuids.get(2)).orElseThrow();

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
                        newStupidStageDefinition(DEF_A,"stage-A", List.of(DEF_C)),
                        newStupidStageDefinition(DEF_B,"stage-B", List.of(DEF_C)),
                        newStupidStageDefinition(DEF_C,"stage-C", null)
                )
        );

        var graph = new Graph(
                emptyPipeline(),
                pipelineDefinition,
                new Node(
                        pipelineDefinition.stages().get(0),
                        null
                )
        );

        var node1 = graph.getNodeForStageDefinitionId(DEF_A).orElseThrow();
        var node2 = graph.getNodeForStageDefinitionId(DEF_B).orElseThrow();
        var node3 = graph.getNodeForStageDefinitionId(DEF_C).orElseThrow();

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
                        pipelineDefinition.stages().get(0),
                        null
                )
        );

        testComplexGraphByDefinitions(graph);
    }

    private static void testComplexGraphByDefinitions(@Nonnull Graph graph) {
        var nodeA = graph.getNodeForStageDefinitionId(DEF_A).orElseThrow();
        var nodeB = graph.getNodeForStageDefinitionId(DEF_B).orElseThrow();
        var nodeC = graph.getNodeForStageDefinitionId(DEF_C).orElseThrow();
        var nodeD = graph.getNodeForStageDefinitionId(DEF_D).orElseThrow();
        var nodeE = graph.getNodeForStageDefinitionId(DEF_E).orElseThrow();
        var nodeF = graph.getNodeForStageDefinitionId(DEF_F).orElseThrow();
        var nodeG = graph.getNodeForStageDefinitionId(DEF_G).orElseThrow();
        var node1 = graph.getNodeForStageDefinitionId(GTW_1).orElseThrow();
        var node2 = graph.getNodeForStageDefinitionId(GTW_2).orElseThrow();
        var node3 = graph.getNodeForStageDefinitionId(GTW_3).orElseThrow();

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
                List.of(nodeB, nodeC),
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

        assertEquals(List.of(exgA), graph.getNodeForStageDefinitionId(defA.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg1), graph.getNodeForStageDefinitionId(gtw1.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgB), graph.getNodeForStageDefinitionId(defB.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgC), graph.getNodeForStageDefinitionId(defC.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg2), graph.getNodeForStageDefinitionId(gtw2.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgD), graph.getNodeForStageDefinitionId(defD.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgE), graph.getNodeForStageDefinitionId(defE.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgF), graph.getNodeForStageDefinitionId(defF.id()).orElseThrow().getExecutionGroups());

        assertContainsAll(
                List.of(exg3E, exg3F, exg3B),
                graph.getNodeForStageDefinitionId(gtw3.id()).orElseThrow().getExecutionGroups()
        );

        assertEquals(List.of(exgG), graph.getNodeForStageDefinitionId(defG.id()).orElseThrow().getExecutionGroups());
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
        var exgC  = emptyExecutionGroup(defC, exg1.getId());
        var exg2  = emptyExecutionGroup(gtw2, exgC.getId());
        var exgD  = emptyExecutionGroup(defD, exg2.getId());
        var exgE  = emptyExecutionGroup(defE, exg2.getId());
        var exgF  = emptyExecutionGroup(defF, exgD.getId());
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

        assertEquals(List.of(exgA), graph.getNodeForStageDefinitionId(defA.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg1), graph.getNodeForStageDefinitionId(gtw1.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgC), graph.getNodeForStageDefinitionId(defC.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exg2), graph.getNodeForStageDefinitionId(gtw2.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgD), graph.getNodeForStageDefinitionId(defD.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgE), graph.getNodeForStageDefinitionId(defE.id()).orElseThrow().getExecutionGroups());
        assertEquals(List.of(exgF), graph.getNodeForStageDefinitionId(defF.id()).orElseThrow().getExecutionGroups());

        assertContainsAll(
                List.of(exg3E, exg3F),
                graph.getNodeForStageDefinitionId(gtw3.id()).orElseThrow().getExecutionGroups()
        );
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
        var nodeGtw1 = graph.getNodeForStageDefinitionId(gtw1.id()).orElseThrow();
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
        return definition.stages().stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
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

                newStupidStageDefinition(DEF_A,"def-a", List.of(GTW_1)),
                newStupidStageDefinition(GTW_1, "gtw-1", List.of(DEF_B, DEF_C)),

                // path gtw-1 upper / path def-c
                newStupidStageDefinition(DEF_C,"def-c", List.of(GTW_2)),

                // path gtw-2 upper
                newStupidStageDefinition(GTW_2, "gtw-2", List.of(DEF_D, DEF_E)),
                newStupidStageDefinition(DEF_E,"def-e", List.of(GTW_3)),

                // path gtw-2 lower
                newStupidStageDefinition(DEF_D,"def-d", List.of(DEF_F)),
                newStupidStageDefinition(DEF_F,"def-f", List.of(GTW_3)),

                // path gtw-1 lower
                newStupidStageDefinition(DEF_B,"def-b", List.of(GTW_3)),


                newStupidStageDefinition(GTW_3, "gtw-3", List.of(DEF_G)),
                newStupidStageDefinition(DEF_G,"def-g", List.of())
        ));
    }

    @Nonnull
    public static Pair<PipelineDefinition, List<UUID>> getDirectPipelineDefinition(@Nonnull String... stageNames) {
        var stages = new ArrayList<StageDefinition>(stageNames.length);
        List<UUID> uuids = Arrays
                .stream(stageNames)
                .map(StageWorkerDefinition::idFromName)
                .collect(Collectors.toList());

        for (int i = 0; i < stageNames.length; ++i) {

            stages.add(newStupidStageDefinition(
                    uuids.get(i),
                    stageNames[i],
                    i + 1 < stageNames.length ? List.of(uuids.get(i + 1)) : null
            ));
        }

        return Pair.with(newStupidPipelineDefinition("the-name", stages), uuids);
    }

    @Nonnull
    public static PipelineDefinition newStupidPipelineDefinition(
            @Nonnull String name,
            @Nonnull List<StageDefinition> stages) {
        return new PipelineDefinition(name, null, null, stages, null, null, null);
    }

    @Nonnull
    public static StageWorkerDefinition newStupidStageDefinition(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nullable List<UUID> next) {

        return new StageWorkerDefinition(
                id,
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
                null
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
