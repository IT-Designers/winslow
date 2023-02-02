package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.GatewaySubType;
import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.web.PipelineDefinitionInfoConverter;
import de.itdesigners.winslow.web.StageDefinitionInfoConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DefaultsController {
    private final Winslow winslow;

    public DefaultsController(Winslow winslow) {
        this.winslow = winslow;
    }


    @GetMapping("/default/worker")
    public StageDefinitionInfo getDefaultWorker() {
        var time = System.currentTimeMillis();
        return StageDefinitionInfoConverter.from(new StageWorkerDefinition(
                UUID.randomUUID(),
                "Worker-" + time,
                new Image("hello-world")
        ));

    }

    @GetMapping("/default/and-splitter")
    public StageDefinitionInfo getDefaultAndSplitter() {
        var time = System.currentTimeMillis();
        return StageDefinitionInfoConverter.from(new StageAndGatewayDefinition(
                UUID.randomUUID(),
                "worker",
                null,
                null,
                GatewaySubType.SPLITTER
        ));
    }

    @GetMapping("/default/all-merger")
    public StageDefinitionInfo getDefaultAllMerger() {
        var time = System.currentTimeMillis();
        return StageDefinitionInfoConverter.from(new StageAndGatewayDefinition(
                UUID.randomUUID(),
                "worker",
                null,
                null,
                GatewaySubType.MERGER
        ));
    }

    @GetMapping("/default/if-splitter")
    public StageDefinitionInfo getDefaultIfSplitter() {
        return StageDefinitionInfoConverter.from(new StageXOrGatewayDefinition(
                UUID.randomUUID(),
                "worker",
                null,
                null,
                null,
                GatewaySubType.SPLITTER
        ));
    }

    @GetMapping("/default/any-merger")
    public StageDefinitionInfo getDefaultAnyMerger() {
        return StageDefinitionInfoConverter.from(new StageXOrGatewayDefinition(
                UUID.randomUUID(),
                "worker",
                null,
                null,
                null,
                GatewaySubType.MERGER
        ));
    }

    @GetMapping("/default/pipeline/{name}")
    public PipelineDefinitionInfo getDefaultPipeline(@PathVariable(name = "name") String pipelineName) {
        return PipelineDefinitionInfoConverter.from(
                pipelineName + "_" + UUID.randomUUID(),
                new PipelineDefinition(pipelineName)
        );
    }
}
