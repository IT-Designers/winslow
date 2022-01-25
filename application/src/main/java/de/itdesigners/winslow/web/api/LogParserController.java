package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.config.LogParser;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.fs.LockException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
public class LogParserController {
    private final        Winslow winslow;
    private static final Logger  LOG = Logger.getLogger(LogParserController.class.getSimpleName());

    public LogParserController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/pipelines/{pipeline}/{stage}/logparsers")
    public Stream<LogParser> getStagesForPipeline(
            @PathVariable(name = "pipeline") String pipeline,
            @PathVariable(name = "stage") String stage) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .stream()
                .flatMap(p -> p.getStages().stream())
                .filter(s -> Objects.equals(s.getName(), stage))
                .flatMap(s -> s.getLogParsers().stream());

    }


    @PutMapping("/pipelines/{pipeline}/{stage}/logparsers")
    public void putStagesForPipeline(
            @PathVariable(name = "pipeline") String pipeline,
            @PathVariable(name = "stage") String stage,
            @RequestBody List<LogParser> logParsers) {
        var exclusivePipelineOpt = winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .exclusive()
                .orElseThrow();

        try (exclusivePipelineOpt) {
            var pipeline_def = exclusivePipelineOpt.getNoThrow().orElseThrow();

            var stageDef = pipeline_def
                    .getStages()
                    .stream()
                    .filter(s -> Objects.equals(s.getName(), stage))
                    .findFirst()
                    .orElseThrow();


            var newStage = new StageDefinition(
                    stageDef.getName(),
                    stageDef.getDescription().orElse(null),
                    stageDef.getImage().orElse(null),
                    stageDef.getRequirements().orElse(null),
                    stageDef.getRequires().orElse(null),
                    stageDef.getEnvironment(),
                    stageDef.getHighlight().orElse(null),
                    stageDef.isDiscardable(),
                    stageDef.isPrivileged(),
                    logParsers,
                    stageDef.getIgnoreFailuresWithinExecutionGroup(),
                    stageDef.getTags()
            );

            var newStages = pipeline_def.getStages().stream().map(s -> {
                if (s == stageDef) {
                    return newStage;
                } else {
                    return s;
                }
            }).collect(Collectors.toList());

            var newPipeline = new PipelineDefinition(
                    pipeline_def.getName(),
                    pipeline_def.getDescription().orElse(null),
                    pipeline_def.getRequires().orElse(null),
                    newStages,
                    pipeline_def.getEnvironment(),
                    pipeline_def.getDeletionPolicy().orElse(null),
                    pipeline_def.getMarkers()
            );

            exclusivePipelineOpt.update(newPipeline);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to update LogParser for stage " + stage + " in Pipeline " + pipeline, e);
        }
    }

}
