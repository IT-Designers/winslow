package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.pipeline.NamedId;
import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

import static de.itdesigners.winslow.config.JsonUpgrade.readNullable;

public class ExecutionGroupUpgrade extends JsonDeserializer<ExecutionGroup> {
    @Override
    public ExecutionGroup deserialize(
            JsonParser p,
            DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        // upgrade from Stage -> ExecutionGroup?
        if (node.has("action")) {
            return upgrade(p, node);
        } else {
            // return p.readValueAs(ExecutionGroup.class);
            return DeserializerUtils.deserializeWithDefaultDeserializer(node, ctxt, ExecutionGroup.class);
        }
    }

    @Nonnull
    private ExecutionGroup upgrade(@Nonnull JsonParser p, @Nonnull JsonNode node) throws IOException {
        var id = NamedId.parseLegacyExecutionGroupId(node.get("id").asText());
        var definition = node
                .get("definition")
                .traverse(p.getCodec())
                .readValueAs(StageWorkerDefinition.class);
        var action    = node.get("action").traverse(p.getCodec()).readValueAs(Action.class);
        var startTime = node.get("startTime").traverse(p.getCodec()).readValueAs(Date.class);
        var workspace = Optional
                .ofNullable(node.get("workspace"))
                .map(JsonNode::asText)
                .orElse(null);
        var finishTime  = readNullable(node, p.getCodec(), "finishTime", Date.class);
        var finishState = readNullable(node, p.getCodec(), "finishState", State.class);
        var env         = readNullable(node, p.getCodec(), "env", Map.class);
        var envPipeline = readNullable(node, p.getCodec(), "envPipeline", Map.class);
        var envSystem   = readNullable(node, p.getCodec(), "envSystem", Map.class);
        var envInternal = readNullable(node, p.getCodec(), "envInternal", Map.class);
        var workspaceConfiguration = Optional
                .ofNullable(readNullable(
                        node,
                        p.getCodec(),
                        "workspaceConfiguration",
                        WorkspaceConfiguration.class
                ))
                .orElseGet(() -> new WorkspaceConfiguration(WorkspaceConfiguration.WorkspaceMode.INCREMENTAL));

        return new ExecutionGroup(
                id,
                Action.CONFIGURE == action,
                definition,
                null,
                workspaceConfiguration,
                new ArrayList<>(List.of(new Stage(
                        id.generateStageId(null),
                        startTime,
                        workspace,
                        finishTime,
                        finishState,
                        env,
                        envPipeline,
                        envSystem,
                        envInternal,
                        new HashMap<>()
                ))),
                0,
                null,
                null
        );
    }
}
