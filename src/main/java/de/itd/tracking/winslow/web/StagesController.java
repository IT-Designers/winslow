package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.config.StageDefinition;
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
                        .getStageDefinitions()
                        .stream()).map(StageInfo::new);
    }

    public static class StageInfo {
        private final String name;

        StageInfo(StageDefinition definition) {
            this.name = definition.getName();
        }

        public String getName() {
            return name;
        }
    }
}
