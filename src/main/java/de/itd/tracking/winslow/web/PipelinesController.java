package de.itd.tracking.winslow.web;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.PipelineDefinitionRepository;
import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.fs.LockException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class PipelinesController {

    private final Winslow winslow;

    public PipelinesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("pipelines")
    public Stream<PipelineInfo> getAllPipelines() {
        return winslow
                .getPipelineRepository()
                .getPipelineIdentifiers()
                .flatMap(identifier -> winslow
                        .getPipelineRepository()
                        .getPipeline(identifier)
                        .unsafe()
                        .stream()
                        .map(p -> new PipelineInfo(identifier, p.getName(), p.getDescription().orElse(null))));
    }

    @GetMapping("pipelines/{pipeline}")
    public Optional<PipelineInfo> getPipeline(@PathVariable("pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .map(p -> new PipelineInfo(pipeline, p.getName(), p.getDescription().orElse(null)));
    }

    @GetMapping("pipelines/{pipeline}/raw")
    public Optional<String> getPipelineRaw(@PathVariable("pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .map(p -> new TomlWriter().write(p));
    }

    @PutMapping("pipelines/{pipeline}/raw")
    public Optional<String> setPipeline(
            @PathVariable("pipeline") String pipeline,
            @RequestParam("raw") String raw) throws IOException {
        PipelineDefinition definition = null;

        try {
            definition = new Toml().read(raw).to(PipelineDefinition.class);
            definition.check();
        } catch (Throwable t) {
            return Optional.of(t.getMessage());
        }


        var exclusive = winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .exclusive();
        if (exclusive.isPresent()) {
            var container = exclusive.get();
            var lock      = container.getLock();

            try (lock) {
                container.update(definition);
            }
        }

        return Optional.empty();
    }

    @PostMapping("pipelines/check-toml")
    public Optional<String> checkPipelineDefToml(@RequestParam("raw") String raw) {
        try {
            new Toml().read(raw).to(PipelineDefinition.class).check();
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of(t.getMessage());
        }
    }

    @PutMapping("pipelines/create")
    public Optional<PipelineInfo> createPipeline(@RequestParam("name") String name) {
        var id = PipelineDefinitionRepository.derivePipelineIdFromName(name);
        return this.winslow
                .getPipelineRepository()
                .getPipeline(id)
                .exclusive()
                .flatMap(container -> {
                    try {
                        if (container.get().isEmpty()) {
                            var def = new PipelineDefinition(name, null, null, Collections.emptyList());
                            container.update(def);
                            return Optional.of(new PipelineInfo(id, name, null));
                        } else {
                            return Optional.empty();
                        }
                    } catch (LockException | IOException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
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
