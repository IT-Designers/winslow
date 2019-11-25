package de.itd.tracking.winslow.fs;

import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.DeletionPolicy;
import de.itd.tracking.winslow.pipeline.Stage;
import org.junit.Test;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ObsoleteWorkspaceFinderTest {

    @Test
    public void testNoThrowWithoutHistory() {
        assertTrue(new ObsoleteWorkspaceFinder(new DeletionPolicy()).collectObsoleteWorkspaces().isEmpty());
        assertTrue(new ObsoleteWorkspaceFinder(new DeletionPolicy())
                           .withExecutionHistory(Collections.emptyList())
                           .collectObsoleteWorkspaces()
                           .isEmpty());
    }

    @Test
    public void testFindsFailedWorkspaces() {
        var history = List.of(
                constructFinishedStage("workspace1", Stage.State.Succeeded),
                constructFinishedStage("workspace2", Stage.State.Failed),
                constructFinishedStage("workspace3", Stage.State.Failed),
                constructFinishedStage("workspace4", Stage.State.Succeeded),
                constructFinishedStage("workspace5", Stage.State.Failed)
        );

        assertTrue(
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
                        .isEmpty()
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
                constructFinishedStage("workspace1", Stage.State.Succeeded),
                constructFinishedStage("workspace2", Stage.State.Failed),
                constructFinishedStage("workspace3", Stage.State.Failed),
                constructFinishedStage("workspace4", Stage.State.Succeeded),
                constructFinishedStage("workspace5", Stage.State.Failed),
                constructFinishedStage("workspace6", Stage.State.Succeeded),
                constructFinishedStage("workspace7", Stage.State.Succeeded)
        );

        assertTrue(
                new ObsoleteWorkspaceFinder(new DeletionPolicy(true, null))
                        .withExecutionHistory(history)
                        .collectObsoleteWorkspaces()
                        .isEmpty()
        );

        var list = new ObsoleteWorkspaceFinder(new DeletionPolicy(true, 1))
                .withExecutionHistory(history)
                .collectObsoleteWorkspaces();

        assertEquals(3, list.size());
        assertEquals(List.of("workspace1", "workspace4", "workspace6"), list);
    }

    @NonNull
    private static Stage constructFinishedStage(
            @NonNull String workspace,
            @NonNull Stage.State finishState) {
        return new Stage(
                "some-id",
                new StageDefinition(
                        "some-definition",
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
}
