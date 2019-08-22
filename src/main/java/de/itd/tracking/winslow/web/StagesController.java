package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class StagesController {

    private final Winslow winslow;

    public StagesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/stages/{pipeline}")
    public Iterable<StageInfo> getStagesForPipeline(@PathVariable(name = "pipeline") String pipeline) {
        return winslow
                .getResourceManager()
                .loadPipeline(pipeline)
                .stream()
                .flatMap(p -> p.getStages().stream())
                .map(s -> new StageInfo(s.getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static class StageInfo {
        private final String name;

        public StageInfo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
