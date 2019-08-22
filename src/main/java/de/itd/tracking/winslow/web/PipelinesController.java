package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class PipelinesController {

    private final Winslow winslow;

    public PipelinesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/pipelines")
    public Iterable<PipelineInfo> getAllPipelines() {
        var pipelines = new ArrayList<PipelineInfo>();
        for (var id : winslow.getResourceManager().getPipelineIdentifiers()) {
            var pipeline = winslow.getResourceManager().loadPipeline(id);
            pipeline.map(p -> new PipelineInfo(
                    id,
                    p.getName(),
                    p.getDescription().orElse(null)
            )).ifPresent(pipelines::add);
        }
        return pipelines;
    }

    public static class PipelineInfo {
        private final String id;
        private final String name;
        private final String desc;

        public PipelineInfo(String id, String name, String description) {
            this.id = id;
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
