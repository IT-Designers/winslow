package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.itdesigners.winslow.config.JsonUpgrade.readNullable;

public class PipelineUpgrade extends JsonDeserializer<Pipeline> {
    @Override
    public Pipeline deserialize(
            JsonParser p,
            DeserializationContext ctxt) throws IOException, JsonProcessingException {

        JsonNode node = p.getCodec().readTree(p);
        // upgrade from Stage -> ExecutionGroup?
        if (node.has("runningStage") || node.has("stageCounter")) {
            var projectId      = node.get("projectId").asText();
            var pauseRequested = node.get("pauseRequested").asBoolean();
            var pauseReason    = readNullable(node, p.getCodec(), "pauseReason", Pipeline.PauseReason.class);
            var resumeNotification = readNullable(
                    node,
                    p.getCodec(),
                    "resumeNotification",
                    Pipeline.ResumeNotification.class
            );
            var enqueuedStages = readNullable(
                    node,
                    p.getCodec(),
                    "enqueuedStages",
                    new TypeReference<List<EnqueuedStage>>() {
                    }
            );
            var completedStages = readNullable(
                    node,
                    p.getCodec(),
                    "completedStages",
                    new TypeReference<List<ExecutionGroup>>() {
                    }
            );
            var deletionPolicy = readNullable(
                    node,
                    p.getCodec(),
                    "deletionPolicy",
                    DeletionPolicy.class
            );
            var strategy     = node.get("strategy").traverse(p.getCodec()).readValueAs(Pipeline.Strategy.class);
            var runningStage = readNullable(node, p.getCodec(), "runningStage", ExecutionGroup.class);
            var stageCounter = readNullable(node, p.getCodec(), "stageCounter", Integer.class);
            var workspace = readNullable(
                    node,
                    p.getCodec(),
                    "workspaceConfigurationMode",
                    WorkspaceConfiguration.WorkspaceMode.class
            );

            var executionCounter = new AtomicInteger(
                    stageCounter != null ? stageCounter
                                         : Optional.ofNullable(completedStages).map(List::size).orElse(0)
                            + (runningStage != null ? 1 : 0)
            );

            var executionQueue =
                    Stream
                            .ofNullable(enqueuedStages)
                            .flatMap(Collection::stream)
                            .map(es -> {
                                switch (es.getAction()) {
                                    case Execute:
                                        return new ExecutionGroup(
                                                new ExecutionGroupId(
                                                        projectId,
                                                        executionCounter.incrementAndGet(),
                                                        es.getDefinition().getName()
                                                ),
                                                es.getDefinition(),
                                                es.getWorkspaceConfiguration()
                                        );
                                    case Configure:
                                        return new ExecutionGroup(
                                                new ExecutionGroupId(
                                                        projectId,
                                                        executionCounter.incrementAndGet(),
                                                        es.getDefinition().getName()
                                                ),
                                                es.getDefinition()
                                        );
                                    default:
                                        throw new RuntimeException("Unexpected action for legacy storage " + es.getAction());
                                }
                            })
                            .collect(Collectors.toList());

            return new Pipeline(
                    projectId,
                    completedStages,
                    executionQueue,
                    runningStage,
                    pauseRequested,
                    pauseReason,
                    resumeNotification,
                    deletionPolicy,
                    strategy,
                    workspace,
                    executionCounter.get()
            );

        } else {
            return p.readValueAs(Pipeline.class);
        }
    }
}
