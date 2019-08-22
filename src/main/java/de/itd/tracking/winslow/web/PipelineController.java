package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.config.Pipeline;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

@RestController
public class PipelineController {

    private final Winslow winslow;

    public PipelineController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/pipelines")
    public Iterable<String> getAllPipelines() {
        var pipelines = new ArrayList<String>();
        for (var path : winslow.listPipelines()) {
            pipelines.add(path.toFile().getName());
        }
        return pipelines;
    }

    @GetMapping("/pipeline/{name}")
    public Optional<Pipeline> getPipeline(@PathVariable("name") String name) {
        return winslow.getResourceManager().loadPipeline(Path.of(name + ".toml"));
    }
}
