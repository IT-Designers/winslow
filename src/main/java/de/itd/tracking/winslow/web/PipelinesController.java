package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@RestController
public class PipelinesController {

    private final Winslow winslow;

    public PipelinesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/pipelines")
    public Stream<PipelineInfo> getAllPipelines() {
        return winslow.getPipelineRepository().getPipelineIdentifiers().flatMap(identifier -> winslow
                .getPipelineRepository()
                .getPipeline(identifier)
                .unsafe()
                .stream()
                .map(p -> new PipelineInfo(identifier, p.getName(), p.getDescription().orElse(null))));
    }

    public static class PipelineInfo {
        private final String id;
        private final String name;
        private final String desc;

        public PipelineInfo(String id, String name, String description) {
            this.id   = id;
            this.name = name;
            this.desc = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }
    }
}
