package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
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
            return upgrade(p, node);
        } else {
            if (node instanceof ObjectNode) {
                ((ObjectNode) node).remove("strategy");
                JsonNode activeExecutionGroup = ((ObjectNode) node).remove("activeExecutionGroup");
                if (activeExecutionGroup != null) {
                    ((ObjectNode) node).putArray("activeExecutionGroups").add(activeExecutionGroup);
                }
                JsonNode activeExecutionGroups = node.get("activeExecutionGroups");
                for (int i = 0; i < activeExecutionGroups.size(); i++) {
                    if (activeExecutionGroups.get(i) == null || activeExecutionGroups.get(i).isNull()) {
                        ((ArrayNode) activeExecutionGroups).remove(i);
                        i--;
                    }
                }
            }
            // return ctxt.readValue(node.traverse(), Pipeline.class);
            return DeserializerUtils.deserializeWithDefaultDeserializer(node, ctxt, Pipeline.class);
        }
    }

    @Nonnull
    private Pipeline upgrade(@Nonnull JsonParser p, @Nonnull JsonNode node) throws IOException {
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
                                            es.getWorkspaceConfiguration(),
                                            es.getComment()
                                              .map(String::trim)
                                              .filter(s -> !s.isEmpty())
                                              .orElse(null),
                                            null
                                    );
                                case Configure:
                                    return new ExecutionGroup(
                                            new ExecutionGroupId(
                                                    projectId,
                                                    executionCounter.incrementAndGet(),
                                                    es.getDefinition().getName()
                                            ),
                                            es.getDefinition(),
                                            es.getComment()
                                              .map(String::trim)
                                              .filter(s -> !s.isEmpty())
                                              .orElse(null)
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
                new ArrayList<>(Optional.ofNullable(runningStage).map(List::of).orElseGet(Collections::emptyList)),
                pauseRequested,
                pauseReason,
                resumeNotification,
                deletionPolicy,
                workspace,
                executionCounter.get()
        );
    }
}
