package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.EnvVariable;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Stage;
import org.junit.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;

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
                        .of(constructFinishedExecutionStage(
                                "some-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history")
                        ), constructFinishedExecutionStage(
                                "some-other-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history-entry-with-wrong-name")
                        ), constructFinishedExecutionStage(
                                "some-stage-name",
                                State.Failed,
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
                        .of(constructFinishedExecutionStage(
                                "some-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history")
                        )));

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                "some-stage-name",
                false,
                Map.of("variable", "enqueued-stage-to-execute")
        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "enqueued-stage-to-execute", "pipeline");

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                "some-stage-name",
                false,
                Map.of("variable", "enqueued-stage-to-execute")
        ), constructEnqueuedSingleExecutionStage(
                "some-stage-name",
                true,
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
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        "some-stage-name",
                        State.Succeeded,
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

    @Test
    public void testConfigureStageOverwritesExecutedStageWithRemovedVariables() {
        var resolved = new EnvVariableResolver()
                .withStageName("some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        "some-stage-name",
                        State.Succeeded,
                        Map.of("executed", "executed")
                ), constructFinishedStageWithAction(
                        "some-stage-name",
                        State.Succeeded,
                        Map.of("configure", "configure"),
                        Action.Configure
                )))
                .resolve();

        assertEquals("configure", resolved.remove("configure").value);
        assertNull(resolved.remove("executed"));
        assertTrue(resolved.isEmpty());
    }

    @Test
    public void testEnqueuedStageOverwritesExecutedStageWithRemovedVariables() {
        var resolved = new EnvVariableResolver()
                .withStageName("some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        "some-stage-name",
                        State.Succeeded,
                        Map.of("executed", "executed")
                )))
                .withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                        "some-stage-name",
                        true,
                        Map.of("enqueued", "enqueued")
                )))
                .resolve();

        assertEquals("enqueued", resolved.remove("enqueued").value);
        assertNull(resolved.remove("executed"));
        assertTrue(resolved.isEmpty());
    }

    private static void assertEnvVariable(
            EnvVariable variable,
            String key,
            String value,
            String inherited) {
        assertNotNull(variable);
        assertEquals(key, variable.key);
        assertEquals(value, variable.value);
        assertEquals(inherited, variable.valueInherited);
    }

    @NonNull
    private static ExecutionGroup constructFinishedExecutionStage(
            @NonNull String name,
            @NonNull State finishState,
            @Nullable Map<String, String> env) {
        return constructFinishedStageWithAction(name, finishState, env, Action.Execute);
    }

    @NonNull
    private static ExecutionGroup constructFinishedStageWithAction(
            @NonNull String name,
            @NonNull State finishState,
            @Nullable Map<String, String> env,
            @Nonnull Action action) {
        return wrap(
                name,
                env,
                (gid) -> new Stage(
                        gid.generateStageId(1),
                        new Date(0L),
                        null,
                        new Date(),
                        finishState,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    @Nonnull
    private static ExecutionGroup wrap(
            @Nonnull String stageDefName,
            @Nullable Map<String, String> env,
            @Nonnull Function<ExecutionGroupId, Stage> stageBuilder) {
        return wrapCustom(stageDefName, env, group -> group.addStage(stageBuilder.apply(group.getId())));
    }

    @Nonnull
    private static ExecutionGroup wrapCustom(
            @Nonnull String stageDefName,
            @Nullable Map<String, String> env,
            @Nonnull Consumer<ExecutionGroup> stuffer) {
        var group = new ExecutionGroup(
                new ExecutionGroupId(
                        "randomish-project",
                        0,
                        "randomish-human-readable"
                ),
                new StageDefinition(
                        stageDefName,
                        null,
                        null,
                        null,
                        null,
                        env,
                        null,
                        null,
                        null,
                        null
                ),
                null
        );
        stuffer.accept(group);
        return group;
    }

    @NonNull
    private static ExecutionGroup constructEnqueuedSingleExecutionStage(
            @Nonnull String stageDefName,
            boolean configureOnly,
            @Nullable Map<String, String> env) {
        return new ExecutionGroup(
                new ExecutionGroupId(
                        "randomish-project",
                        0,
                        "randomish-human-readable"
                ),
                configureOnly,
                new StageDefinition(
                        stageDefName,
                        null,
                        null,
                        null,
                        null,
                        env,
                        null,
                        null,
                        null,
                        null
                ),
                null,
                new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL, null, null),
                new ArrayList<>(),
                0,
                null
        );
    }
}
