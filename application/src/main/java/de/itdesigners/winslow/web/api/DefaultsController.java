package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.RequirementsInfo;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.web.PipelineDefinitionInfoConverter;
import de.itdesigners.winslow.web.StageDefinitionInfoConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.UUID;

@RestController
public class DefaultsController {
    private final Winslow winslow;

    public DefaultsController(Winslow winslow) {
        this.winslow = winslow;
    }


    @GetMapping("/default/worker")
    public StageDefinitionInfo getDefaultWorker() {
        return StageDefinitionInfoConverter.from(new StageWorkerDefinition(
                UUID.randomUUID(),
                "Worker-" + System.currentTimeMillis(),
                "A new worker was born",
                new Image(
                        "hello-world",
                        new String[0],
                        0L
                ),
                new Requirements(
                        null,
                        null,
                        null,
                        null
                ),
                new UserInput(null, null),
                Collections.emptyMap(),
                null,
                false,
                false,
                Collections.emptyList(),
                false,
                Collections.emptyList()
        ));

    }

    @GetMapping("/and-gateway")
    public StageDefinitionInfo getDefaultAndGateway() {

        return StageDefinitionInfoConverter.from(new StageAndGatewayDefinition(
                null,
                "worker",
                null,
                null
        ));

    }

    @GetMapping("/default/xor-gateway")
    public StageDefinitionInfo getDefaultXOrGateway() {

        return StageDefinitionInfoConverter.from(new StageXOrGatwayDefinition(
                null,
                "worker",
                null,
                null,
                null
        ));

    }

    @GetMapping("/default/pipeline/{name}")
    public PipelineDefinitionInfo getDefaultPipeline(@PathVariable(name = "pipeline") String pipelineName) {

        return PipelineDefinitionInfoConverter.from(
                pipelineName + "_" + UUID.randomUUID(),
                new PipelineDefinition(
                        pipelineName,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );
    }
}
