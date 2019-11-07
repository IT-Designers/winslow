package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.config.UserInput;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
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
                        .stream()).map(StageInfo::new);
    }

    public static class StageInfo {
        @Nonnull public final  String                       name;
        @Nullable public final ProjectsController.ImageInfo image;
        @Nonnull public final  List<String>                 requiredEnvVariables;

        StageInfo(StageDefinition definition) {
            this.name                 = definition.getName();
            this.image                = definition.getImage().map(ProjectsController.ImageInfo::new).orElse(null);
            this.requiredEnvVariables = definition
                    .getRequires()
                    .map(UserInput::getEnvironment)
                    .orElseGet(Collections::emptyList);
        }
    }
}
