package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.StageXOrGatwayDefinition;
import de.itdesigners.winslow.web.StageDefinitionInfoConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@RestController
public class StagesController {

    private final Winslow winslow;

    public StagesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/stages/default/worker")
    public StageDefinitionInfo getDefaultWorker() {

        return StageDefinitionInfoConverter.from(new StageWorkerDefinition(
                null,
                "worker",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

    }

    @GetMapping("/stages/default/andGateway")
    public StageDefinitionInfo getDefaultAndGateway() {

        return StageDefinitionInfoConverter.from(new StageAndGatewayDefinition(
                null,
                "worker",
                null,
                null
        ));

    }

    @GetMapping("/stages/default/xorGateway")
    public StageDefinitionInfo getDefaultXOrGateway() {

        return StageDefinitionInfoConverter.from(new StageXOrGatwayDefinition(
                null,
                "worker",
                null,
                null,
                null
        ));

    }

    @GetMapping("/stages/{pipeline}")
    public Stream<StageDefinitionInfo> getStagesForPipeline(@PathVariable(name = "pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .stream()
                .flatMap(p -> p.stages().stream())
                .map(StageDefinitionInfoConverter::from);

    }

}
