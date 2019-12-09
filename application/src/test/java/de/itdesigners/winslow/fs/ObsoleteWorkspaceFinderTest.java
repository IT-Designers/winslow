package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.DeletionPolicy;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Stage;
import org.junit.Test;
import org.springframework.lang.NonNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ObsoleteWorkspaceFinderTest {

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

        assertEquals(3, list.size());
        assertEquals(List.of("workspace2", "workspace3", "workspace5"), list);
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

        assertEquals(3, list.size());
        assertEquals(List.of("workspace1", "workspace4", "workspace6"), list);
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
                constructFinishedDiscardableStage("workspace1", Action.Execute, State.Succeeded),
                constructFinishedDiscardableStage("workspace2", Action.Execute, State.Succeeded),
                constructFinishedDiscardableStage("workspace3", Action.Execute, State.Failed),
                constructFinishedDiscardableStage("workspace4", Action.Execute, State.Succeeded)
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
                constructFinishedDiscardableStage("workspace1", Action.Execute, State.Succeeded),
                constructFinishedDiscardableStage("workspace2", Action.Execute, State.Failed)
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
                constructFinishedDiscardableStage("workspace1", Action.Execute, State.Succeeded),
                constructFinishedDiscardableStage("workspace2", Action.Configure, State.Succeeded)
        );

        assertEquals(
                Collections.emptyList(),
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
        );
    }


    @NonNull
    private static Stage constructFinishedStage(
            @NonNull String workspace,
            @NonNull State finishState) {
        return new Stage(
                "some-id",
                new StageDefinition(
                        "some-definition",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                Action.Execute,
                new Date(0L),
                workspace,
                new Date(),
                finishState,
                null,
                null,
                null,
                null
        );
    }

    @NonNull
    private static Stage constructFinishedConfigureStage(
            @NonNull String workspace,
            @NonNull State finishState) {
        return new Stage(
                "some-id",
                new StageDefinition(
                        "some-definition",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                Action.Configure,
                new Date(0L),
                workspace,
                new Date(),
                finishState,
                null,
                null,
                null,
                null
        );
    }

    @NonNull
    private static Stage constructFinishedDiscardableStage(
            @NonNull String workspace,
            @Nonnull Action action,
            @NonNull State finishState) {
        return new Stage(
                "some-id",
                new StageDefinition(
                        "some-definition",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Boolean.TRUE
                ),
                action,
                new Date(0L),
                workspace,
                new Date(),
                finishState,
                null,
                null,
                null,
                null
        );
    }
}
