package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
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
