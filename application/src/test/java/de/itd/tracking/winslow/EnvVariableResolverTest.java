package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.EnqueuedStage;
import de.itd.tracking.winslow.pipeline.Stage;
import de.itd.tracking.winslow.web.ProjectsController;
import org.junit.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class EnvVariableResolverTest {

    @Test
    public void testPipelineDefinitionOverwritesGlobalEnvironment() {
        var resolved = new EnvVariableResolver()
                .withGlobalVariables(Map.of("variable", "global"))
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .resolve();

        assertEnvVariable(resolved.get("variable"), "variable", "pipeline", "global");
    }

    @Test
    public void testVariablesOfStageDefinitionOverwriteVariablesOfPipelineDefinition() {
        var resolved = new EnvVariableResolver()
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("variable", "stage"))
                .resolve();

        assertEnvVariable(resolved.get("variable"), "variable", "stage", "pipeline");
    }

    @Test
    public void testHistoryOverwritesVariablesOfStageDefinition() {
        var resolver = new EnvVariableResolver()
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("variable", "stage")) // this is expected to 'disappear'
                .withStageName("some-stage-name")
                .withExecutionHistory(() -> Stream
                        .of(constructFinishedStage(
                                "some-stage-name",
                                Stage.State.Succeeded,
                                Map.of("variable", "history")
                        ), constructFinishedStage(
                                "some-other-stage-name",
                                Stage.State.Succeeded,
                                Map.of("variable", "history-entry-with-wrong-name")
                        ), constructFinishedStage(
                                "some-stage-name",
                                Stage.State.Failed,
                                Map.of("variable", "history-entry-which-failed")
                        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "history-entry-which-failed", "pipeline");
    }

    @Test
    public void testEnQueuedOverwritesHistoryOverwritesVariablesOfStageDefinition() {
        var resolver = new EnvVariableResolver()
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("variable", "stage")) // this is expected to 'disappear'
                .withStageName("some-stage-name")
                .withExecutionHistory(() -> Stream
                        .of(constructFinishedStage(
                                "some-stage-name",
                                Stage.State.Succeeded,
                                Map.of("variable", "history")
                        )));

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedStage(
                "some-stage-name",
                Action.Execute,
                Map.of("variable", "enqueued-stage-to-execute")
        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "enqueued-stage-to-execute", "pipeline");

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedStage(
                "some-stage-name",
                Action.Execute,
                Map.of("variable", "enqueued-stage-to-execute")
        ), constructEnqueuedStage(
                "some-stage-name",
                Action.Configure,
                Map.of("variable", "enqueued-stage-to-configure")
        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "enqueued-stage-to-configure", "pipeline");
    }

    @Test
    public void testPreservesUnrelatedVariables() {
        var resolved = new EnvVariableResolver()
                .withGlobalVariables(Map.of("global", "global"))
                .withInPipelineDefinitionDefinedVariables(Map.of("pipeline", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("stage", "stage"))
                .withStageName("some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedStage(
                        "some-stage-name",
                        Stage.State.Succeeded,
                        Map.of("history", "history")
                )))
                .resolve();

        for (var check : Arrays.asList("global", "pipeline")) {
            assertEnvVariable(resolved.get(check), check, check, check);
        }

        for (var check : Arrays.asList("stage", "history")) {
            assertEnvVariable(resolved.get(check), check, check, null);
        }
    }

    private static void assertEnvVariable(
            ProjectsController.EnvVariable variable,
            String key,
            String value,
            String inherited) {
        assertNotNull(variable);
        assertEquals(key, variable.key);
        assertEquals(value, variable.value);
        assertEquals(inherited, variable.valueInherited);
    }

    @NonNull
    private static Stage constructFinishedStage(
            @NonNull String name,
            @NonNull Stage.State finishState,
            @Nullable Map<String, String> env) {
        return new Stage(
                "some-id",
                new StageDefinition(
                        name,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                Action.Execute,
                new Date(0L),
                null,
                new Date(),
                finishState,
                env,
                null,
                null,
                null
        );
    }

    @NonNull
    private static EnqueuedStage constructEnqueuedStage(
            @NonNull String name,
            @Nonnull Action action,
            @Nullable Map<String, String> env) {
        return new EnqueuedStage(
                new StageDefinition(
                        name,
                        null,
                        null,
                        null,
                        null,
                        env,
                        null
                ),
                action
        );
    }
}
