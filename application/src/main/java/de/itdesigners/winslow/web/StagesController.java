package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.StageInfo;
import de.itdesigners.winslow.config.StageDefinition;
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

    @GetMapping("/stages/{pipeline}")
    public Stream<StageInfo> getStagesForPipeline(@PathVariable(name = "pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .stream()
                .flatMap(p -> p
                        .getStages()
                        .stream()).map((StageDefinition t) -> new StageInfo(name, image, requiredEnvVariables));
    }

}
