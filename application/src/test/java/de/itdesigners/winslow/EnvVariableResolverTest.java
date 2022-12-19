package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.EnvVariable;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Stage;
import org.junit.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
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
        var uuid = UUID.randomUUID();
        var resolver = new EnvVariableResolver()
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("variable", "stage")) // this is expected to 'disappear'
                .withIdAndStageName(uuid, "some-stage-name")
                .withExecutionHistory(() -> Stream
                        .of(constructFinishedExecutionStage(
                                uuid,
                                "some-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history")
                        ), constructFinishedExecutionStage(
                                UUID.randomUUID(),
                                "some-other-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history-entry-with-wrong-name")
                        ), constructFinishedExecutionStage(
                                uuid,
                                "some-stage-name",
                                State.Failed,
                                Map.of("variable", "history-entry-which-failed")
                        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "history-entry-which-failed", "pipeline");
    }

    @Test
    public void testEnQueuedOverwritesHistoryOverwritesVariablesOfStageDefinition() {
        var uuid = UUID.randomUUID();
        var resolver = new EnvVariableResolver()
                .withInPipelineDefinitionDefinedVariables(Map.of("variable", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("variable", "stage")) // this is expected to 'disappear'
                .withIdAndStageName(uuid,"some-stage-name")
                .withExecutionHistory(() -> Stream
                        .of(constructFinishedExecutionStage(
                                uuid,
                                "some-stage-name",
                                State.Succeeded,
                                Map.of("variable", "history")
                        )));

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                uuid,
                "some-stage-name",
                false,
                Map.of("variable", "enqueued-stage-to-execute")
        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "enqueued-stage-to-execute", "pipeline");

        resolver = resolver.withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                uuid,
                "some-stage-name",
                false,
                Map.of("variable", "enqueued-stage-to-execute")
        ), constructEnqueuedSingleExecutionStage(
                uuid,
                "some-stage-name",
                true,
                Map.of("variable", "enqueued-stage-to-configure")
        )));

        assertEnvVariable(resolver.resolve().get("variable"), "variable", "enqueued-stage-to-configure", "pipeline");
    }

    @Test
    public void testPreservesUnrelatedVariables() {
        var uuid = UUID.randomUUID();
        var resolved = new EnvVariableResolver()
                .withGlobalVariables(Map.of("global", "global"))
                .withInPipelineDefinitionDefinedVariables(Map.of("pipeline", "pipeline"))
                .withInStageDefinitionDefinedVariables(Map.of("stage", "stage"))
                .withIdAndStageName(uuid,"some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        uuid,
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
        var uuid = UUID.randomUUID();
        var resolved = new EnvVariableResolver()
                .withIdAndStageName(uuid,"some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        uuid,
                        "some-stage-name",
                        State.Succeeded,
                        Map.of("executed", "executed")
                ), constructFinishedStageWithAction(
                        uuid,
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
        var uuid = UUID.randomUUID();
        var resolved = new EnvVariableResolver()
                .withIdAndStageName(uuid,"some-stage-name")
                .withExecutionHistory(() -> Stream.of(constructFinishedExecutionStage(
                        uuid,
                        "some-stage-name",
                        State.Succeeded,
                        Map.of("executed", "executed")
                )))
                .withEnqueuedStages(() -> Stream.of(constructEnqueuedSingleExecutionStage(
                        uuid,
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
            @Nonnull UUID id,
            @NonNull String name,
            @NonNull State finishState,
            @Nullable Map<String, String> env) {
        return constructFinishedStageWithAction(id, name, finishState, env, Action.Execute);
    }

    @NonNull
    private static ExecutionGroup constructFinishedStageWithAction(
            @NonNull UUID id,
            @NonNull String name,
            @NonNull State finishState,
            @Nullable Map<String, String> env,
            @Nonnull Action action) {
        return wrap(
                id,
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
                        null,
                        null
                )
        );
    }

    @Nonnull
    private static ExecutionGroup wrap(
            @Nonnull UUID stageDefId,
            @Nonnull String stageDefName,
            @Nullable Map<String, String> env,
            @Nonnull Function<ExecutionGroupId, Stage> stageBuilder) {
        return wrapCustom(stageDefId, stageDefName, env, group -> group.addStage(stageBuilder.apply(group.getId())));
    }

    @Nonnull
    private static ExecutionGroup wrapCustom(
            @Nonnull UUID stageDefId,
            @Nonnull String stageDefName,
            @Nullable Map<String, String> env,
            @Nonnull Consumer<ExecutionGroup> stuffer) {
        var group = new ExecutionGroup(
                new ExecutionGroupId(
                        "randomish-project",
                        0,
                        "randomish-human-readable"
                ),
                new StageWorkerDefinition(
                        stageDefId,
                        stageDefName,
                        (String) null,
                        new Image("hello-world"),
                        new Requirements(),
                        new UserInput(),
                        env,
                        null,
                        false,
                        false,
                        null,
                        false,
                        null
                ),
                null
        );
        stuffer.accept(group);
        return group;
    }

    @NonNull
    private static ExecutionGroup constructEnqueuedSingleExecutionStage(
            @Nonnull UUID stageDefId,
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
                new StageWorkerDefinition(
                        stageDefId,
                        stageDefName,
                        (String) null,
                        new Image("hello-world"),
                        new Requirements(),
                        new UserInput(),
                        env,
                        null,
                        false,
                        false,
                        null,
                        false,
                        null
                ),
                null,
                new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL, null, null, null),
                new ArrayList<>(),
                0,
                null,
                null
        );
    }
}
