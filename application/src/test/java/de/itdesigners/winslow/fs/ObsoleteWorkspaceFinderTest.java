package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.RangedList;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Stage;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class ObsoleteWorkspaceFinderTest {

    @Test
    public void testRemoveDuplicatesKeepReverseOrder() {
        var list = new ArrayList<>(List.of(
                "workspace1",
                "workspace1",
                "workspace2",
                "workspace2",
                "workspace2",
                "workspace1"
        ));
        ObsoleteWorkspaceFinder.removeDuplicatesKeepReverseOrder(list, String::equals);
        assertEquals(2, list.size());
        assertEquals("workspace2", list.get(0));
        assertEquals("workspace1", list.get(1));
    }

    @Test
    public void testConsiderContinuedWorkspaces() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded), // cannot because first successful normal (counter)
                constructFinishedStage("workspace2", State.Failed), // cannot be deleted because of below
                constructFinishedStage("workspace2", State.Failed), // cannot be deleted because of below
                constructFinishedDiscardableStage(false, "workspace2", State.Succeeded), // cannot be discarded because first successful one (ignores counter)
                constructFinishedStage("workspace3", State.Failed) // cannot be deleted because missing follow-up
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 1))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        assertEquals(
                obsolete,
                Collections.emptyList()
        );
    }

    @Test
    public void testConsiderContinuedWorkspacesBasic() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),// keep: because below
                constructFinishedStage("workspace1", State.Succeeded),// keep: because below
                constructFinishedStage("workspace2", State.Succeeded),// keep: because below
                constructFinishedStage("workspace2", State.Succeeded),// keep: because below
                constructFinishedStage("workspace2", State.Succeeded),// keep: within range
                constructFinishedStage("workspace1", State.Succeeded) // keep: within range
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 2))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of());
        assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);

    }


    @Test
    public void testConsiderContinuedWorkspacesBasic2() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),// keep: because below
                constructFinishedStage("workspace1", State.Succeeded),// keep: because below
                constructFinishedStage("workspace2", State.Succeeded),// delete: because below
                constructFinishedStage("workspace2", State.Succeeded),// delete: because below
                constructFinishedStage("workspace2", State.Succeeded),// delete: not in range
                constructFinishedStage("workspace1", State.Succeeded) // keep: within range
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 1))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace2"));
        assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);

    }

    @Test
    public void testConsiderContinuedWorkspacesComplex1() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),
                // delete: third successful
                constructFinishedStage("workspace2", State.Failed),
                // keep: because of below
                constructFinishedStage("workspace2", State.Failed),
                // keep: because of below
                constructFinishedDiscardableStage(false, "workspace2", State.Succeeded),
                // keep: because of below
                constructFinishedStage("workspace2", State.Succeeded),
                // keep: second successful
                constructFinishedStage("workspace3", State.Succeeded),
                // keep: because of below
                constructFinishedStage("workspace3", State.Succeeded),
                // keep: first successful
                constructFinishedStage("workspace4", State.Failed)
                // keep: most recent without successful follow-up
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 2))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace1"));
        assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);

    }

    @Test
    public void testConsiderContinuedWorkspacesComplex2() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded), // keep: second non-discardable successful
                constructFinishedStage("workspace2", State.Failed), // delete: because of below
                constructFinishedStage("workspace2", State.Failed), // delete: because of below
                constructFinishedDiscardableStage(false, "workspace2", State.Succeeded), // delete: discardable with succcessful follow-up
                constructFinishedStage("workspace3", State.Succeeded), // keep:  first successful
                constructFinishedStage("workspace3", State.Failed), // keep: because of above
                constructFinishedStage("workspace4", State.Failed) // keep: most recent without successful follow-up
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 2))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace2"));
        assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);

    }

    @Test
    public void testConsiderContinuedWorkspacesComplex2NoAlwaysKeep() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded), // keep: second non-discardable successful
                constructFinishedStage("workspace2", State.Failed), // delete: because of below
                constructFinishedStage("workspace2", State.Failed), // delete: because of below
                constructFinishedDiscardableStage(false, "workspace2", State.Succeeded), // delete: discardable with succcessful follow-up
                constructFinishedStage("workspace3", State.Succeeded), // keep:  first successful
                constructFinishedStage("workspace3", State.Failed), // keep: because of above
                constructFinishedStage("workspace4", State.Failed) // delete: because no keep of most recent
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 2, false))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace2", "workspace4"));
        // assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);

    }


    @Test
    public void testNoThrowWithoutHistory() {
        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy()).collectObsoleteWorkspaces()
        );
        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy())
                        .withExecutionHistory(Collections.emptyList())
                        .collectObsoleteWorkspaces()
        );
    }

    @Test
    public void testFindsFailedWorkspaces() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),
                constructFinishedStage("workspace2", State.Failed),
                constructFinishedStage("workspace3", State.Failed),
                constructFinishedStage("workspace4", State.Succeeded),
                constructFinishedStage("workspace5", State.Failed)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );

        var list = new ObsoleteWorkspaceFinder(new DeletionPolicy(false, null))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace2", "workspace3"));
        assertEquals(expected.size(), list.size());
        expected.removeAll(list);
        assertEquals(
                Collections.emptyList(),
                expected
        );
    }

    @Test
    public void testFindsLimitExceedingWorkspaces() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),
                constructFinishedStage("workspace2", State.Failed),
                constructFinishedStage("workspace3", State.Failed),
                constructFinishedStage("workspace4", State.Succeeded),
                constructFinishedStage("workspace5", State.Failed),
                constructFinishedStage("workspace6", State.Succeeded),
                constructFinishedStage("workspace7", State.Succeeded)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );

        var list = new ObsoleteWorkspaceFinder(new DeletionPolicy(true, 1))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        assertEquals(6, list.size());
        assertEquals(
                List.of(
                        "workspace1",
                        "workspace2",
                        "workspace3",
                        "workspace4",
                        "workspace5",
                        "workspace6"
                ),
                list
        );
    }

    @Test
    public void testIgnoresConfigureStages() {
        var history = List.of(
                constructFinishedStage("workspace1", State.Succeeded),
                constructFinishedConfigureStage("configure1", State.Succeeded),
                constructFinishedConfigureStage("configure2", State.Failed)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 1))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }

    @Test
    public void testProperlyConsidersDiscardable() {
        var history = List.of(
                constructFinishedDiscardableStage(false, "workspace1", State.Succeeded),
                constructFinishedDiscardableStage(false, "workspace2", State.Succeeded),
                constructFinishedDiscardableStage(false, "workspace3", State.Failed),
                constructFinishedDiscardableStage(false, "workspace4", State.Succeeded)
        );

        var obsolete = new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        var expected = new ArrayList<>(List.of("workspace1", "workspace2", "workspace3"));
        assertEquals(expected.size(), obsolete.size());
        expected.removeAll(obsolete);
        assertEquals(Collections.emptyList(), expected);
    }

    @Test
    public void testProperlyConsidersDiscardableAndDoesNotDeleteStagesWithoutSuccessfulFollowups() {
        var history = List.of(
                constructFinishedDiscardableStage(false, "workspace1", State.Succeeded),
                constructFinishedDiscardableStage(false, "workspace2", State.Failed)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }

    @Test
    public void testProperlyConsidersDiscardableAndDoesNotDeleteWhenFollowUpIsAConfigureStage() {
        var history = List.of(
                constructFinishedDiscardableStage(false, "workspace1", State.Succeeded),
                constructFinishedDiscardableStage(true, "workspace2", State.Succeeded)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }

    @Test
    public void testProperlyConsidersDiscardableAndDoesNotDeleteParentDirectoryIfNested() {
        var history = List.of(
                constructFinishedStageWithNestedWorkspaces(false, true, List.of("w1/s1", "w1/s2"), State.Succeeded),
                constructFinishedStage(false, true, "w-broken/s1", State.Succeeded),
                // this one has set nested to true, but has no RangedEnvironmentVariables, the path "w2" must not be substituted to its parent
                constructFinishedStage(false, true, "w2", State.Succeeded, new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.STANDALONE, null, false, true)),
                // this one has set nested to true, but has no RangedEnvironmentVariables, the path "w3/test" must not be substituted to its parent
                constructFinishedStage(false, true, "w3/test", State.Succeeded, new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.STANDALONE, null, false, true)),
                constructFinishedStage(false, true, "w4", State.Succeeded),
                constructFinishedStageWithNestedWorkspaces(false, true, List.of("w5/s1", "w5/s2"), State.Failed)
        );

        assertEquals(
                List.of("w1", "w-broken/s1", "w2", "w3/test"),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 1))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }

    @Test
    public void testNotDeleteParentDirectoryIfNested() {
        var history = List.of(
                constructFinishedStageWithNestedWorkspaces(false, true, List.of("w1/s1", "w1/s2"), State.Succeeded),
                constructFinishedStage(false, false, "w-broken/s1", State.Succeeded),
                // this one has set nested to true, but has no RangedEnvironmentVariables, the path "w2" must not be substituted to its parent
                constructFinishedStage(false, false, "w2", State.Succeeded, new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.STANDALONE, null, false, true)),
                // this one has set nested to true, but has no RangedEnvironmentVariables, the path "w3/test" must not be substituted to its parent
                constructFinishedStage(false, false, "w3/test", State.Succeeded, new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.STANDALONE, null, false, true)),
                constructFinishedStage(false, false, "w4", State.Succeeded),
                constructFinishedStageWithNestedWorkspaces(false, true, List.of("w5/s1", "w5/s2"), State.Failed)
        );

        assertEquals(
                List.of("w1"),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(false, 4))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }


    @Nonnull
    private static ExecutionGroup constructFinishedStage(
            @Nonnull String workspace,
            @Nonnull State finishState) {
        return constructFinishedStage(false, null, workspace, finishState);
    }

    @Nonnull
    private static ExecutionGroup constructFinishedConfigureStage(
            @Nonnull String workspace,
            @Nonnull State finishState) {
        return constructFinishedStage(true, null, workspace, finishState);
    }

    @Nonnull
    private static ExecutionGroup constructFinishedDiscardableStage(
            boolean configureOnly,
            @Nonnull String workspace,
            @Nonnull State finishState) {
        return constructFinishedStage(configureOnly, true, workspace, finishState);
    }

    @Nonnull
    private static ExecutionGroup constructFinishedStage(
            boolean configureOnly,
            @Nullable Boolean discardable,
            @Nonnull String workspace,
            @Nonnull State finishState) {
        return constructFinishedStage(configureOnly, discardable, workspace, finishState, null);
    }

    @Nonnull
    private static ExecutionGroup constructFinishedStage(
            boolean configureOnly,
            @Nullable Boolean discardable,
            @Nonnull String workspace,
            @Nonnull State finishState,
            @Nullable WorkspaceConfiguration workspaceConfiguration) {
        var group = new ExecutionGroup(
                new ExecutionGroupId(
                        "randomish-project",
                        0,
                        "randomish-human-readable"
                ),
                configureOnly,
                new StageDefinition(
                        "some-definition",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        discardable,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                null,
                workspaceConfiguration != null
                    ? workspaceConfiguration
                    : new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL, null, null, null),
                new ArrayList<>(),
                0,
                null
        );
        group.addStage(new Stage(
                group.getId().generateStageId(0),
                new Date(0),
                workspace,
                new Date(),
                finishState,
                null,
                null,
                null,
                null,
                null
        ));
        return group;
    }

    @Nonnull
    private static ExecutionGroup constructFinishedStageWithNestedWorkspaces(
            boolean configureOnly,
            @Nullable Boolean discardable,
            @Nonnull Iterable<String> workspaces,
            @Nonnull State finishState) {
        var group = new ExecutionGroup(
                new ExecutionGroupId(
                        "randomish-project",
                        0,
                        "randomish-human-readable"
                ),
                configureOnly,
                new StageDefinition(
                        "some-definition",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        discardable,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                Map.of("a", new RangedList(new String[]{"b", "c"})),
                new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL, null, null, true),
                new ArrayList<>(),
                0,
                null
        );

        var stageNumberWithinGroup = 0;
        for (var workspace : workspaces) {
            group.addStage(new Stage(
                    group.getId().generateStageId(stageNumberWithinGroup++),
                    new Date(0),
                    workspace,
                    new Date(),
                    finishState,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return group;
    }
}
