package de.itdesigners.winslow;

import de.itdesigners.winslow.api.project.EnvVariable;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.Stage;
import org.springframework.lang.NonNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This class resolves for a given environment, the most recent
 * values of applicable environment variables with proper inheritance
 * relation (see {@link EnvVariable}).
 */
public class EnvVariableResolver {

    private @Nullable Map<String, String>              globalVariables;
    private @Nullable Map<String, String>              pipelineDefinitionVariables;
    private @Nullable Map<String, String>              stageDefinitionVariables;
    private @Nullable Supplier<Stream<ExecutionGroup>> executionHistory;
    private @Nullable Supplier<Stream<EnqueuedStage>>  enqueuedStages;
    private @Nullable String                           stageName;

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withGlobalVariables(@Nullable Map<String, String> globalVariables) {
        this.globalVariables = globalVariables;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withInPipelineDefinitionDefinedVariables(@Nullable Map<String, String> pipelineVariables) {
        this.pipelineDefinitionVariables = pipelineVariables;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withInStageDefinitionDefinedVariables(@Nullable Map<String, String> stageVariables) {
        this.stageDefinitionVariables = stageVariables;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withExecutionHistory(@Nullable Supplier<Stream<ExecutionGroup>> history) {
        this.executionHistory = history;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withEnqueuedStages(@Nullable Supplier<Stream<EnqueuedStage>> enqueued) {
        this.enqueuedStages = enqueued;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public EnvVariableResolver withStageName(@Nullable String stageName) {
        this.stageName = stageName;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public Map<String, EnvVariable> resolve() {
        var result = new TreeMap<String, EnvVariable>();

        // apply all the global environment variables first, so they can get overwritten
        // by any further appearing entry. Mark as inherited because their origin is outside
        // of the stage definition
        if (this.globalVariables != null) {
            pushInherited(result, this.globalVariables);
        }

        // apply all the pipeline environment variables next, they are similar to global
        // environment variables but have a tighter scope and therefore overwrite the more
        // general global environment variables. Mark as inherited because their origin is
        // still outside of the stage definition
        if (this.pipelineDefinitionVariables != null) {
            pushInherited(result, this.pipelineDefinitionVariables);
        }

        // the environment variables defined for the stage have the tightest scope
        // and therefore can overwrite all the others
        pushValue(result, retrieveStageEnvironmentVariables());

        return result;
    }

    @NonNull
    @CheckReturnValue
    private Map<String, String> retrieveStageEnvironmentVariables() {
        var result = new TreeMap<String, String>();

        // the variables defined in the stage definition are the base
        // therefore they need to be included first
        if (this.stageDefinitionVariables != null) {
            result.putAll(this.stageDefinitionVariables);
        }

        // once a stage has been executed, its configuration
        // has a higher relevance and therefore it overwrites the configuration
        // from the definition
        var mostRecentExecuted = retrieveMostRecentExecutedStageEnvironmentVariables();

        // an enqueued stage is even more recent and therefore even more important
        var mostRecentEnqueued = retrieveMostRecentEnqueuedStageEnvironmentVariables();

        result.putAll(
                mostRecentEnqueued
                        .or(() -> mostRecentExecuted)
                        .orElseGet(Collections::emptyMap)
        );

        return result;
    }

    @NonNull
    @CheckReturnValue
    private Optional<Map<String, String>> retrieveMostRecentExecutedStageEnvironmentVariables() {
        if (this.executionHistory != null && this.stageName != null) {
            return this.executionHistory
                    .get()
                    .filter(group -> group.getStageDefinition().getName().equals(this.stageName))
                    .flatMap(ExecutionGroup::getStages)
                    //.filter(s -> s.getFinishState().map(state -> Stage.State.Succeeded == state).orElse(Boolean.FALSE))
                    //.filter(s -> this.stageName.equals(s.getDefinition().getName()))
                    .reduce((first, second) -> second) // expect in order
                    .map(Stage::getEnv);
        } else {
            return Optional.empty();
        }
    }

    @NonNull
    @CheckReturnValue
    private Optional<Map<String, String>> retrieveMostRecentEnqueuedStageEnvironmentVariables() {
        if (this.enqueuedStages != null && this.stageName != null) {
            return this.enqueuedStages
                    .get()
                    .filter(s -> this.stageName.equals(s.getDefinition().getName()))
                    .reduce((first, second) -> second) // expect in order
                    .map(enqueued -> enqueued.getDefinition().getEnvironment());
        } else {
            return Optional.empty();
        }
    }

    private static void pushInherited(
            @Nonnull Map<String, EnvVariable> target,
            @NonNull Map<String, String> source) {
        push(target, source, EnvVariable::new);
    }

    private static void pushValue(
            @Nonnull Map<String, EnvVariable> target,
            @NonNull Map<String, String> source) {
        push(target, source, (key, value) -> new EnvVariable(key));
    }

    private static void push(
            @Nonnull Map<String, EnvVariable> target,
            @NonNull Map<String, String> source,
            @NonNull BiFunction<String, String, EnvVariable> creator) {
        source.forEach((key, value) -> target.computeIfAbsent(key, kl -> creator.apply(key, value)).pushValue(value));
    }

}
