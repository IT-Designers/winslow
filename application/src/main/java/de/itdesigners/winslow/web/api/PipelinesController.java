package de.itdesigners.winslow.web.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import de.itdesigners.winslow.PipelineDefinitionRepository;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ParseError;
import de.itdesigners.winslow.api.pipeline.PipelineInfo;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.web.PipelineInfoConverter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                        .map(p -> PipelineInfoConverter.from(identifier, p)));
    }

    @GetMapping("pipelines/{pipeline}")
    public Optional<PipelineInfo> getPipeline(@PathVariable("pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafe()
                .map(p -> PipelineInfoConverter.from(pipeline, p));
    }

    @GetMapping("pipelines/{pipeline}/raw")
    public Optional<String> getPipelineRaw(@PathVariable("pipeline") String pipeline) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipeline)
                .unsafeRaw();
    }

    @PutMapping("pipelines/{pipeline}/raw")
    public ResponseEntity<String> setPipeline(
            @PathVariable("pipeline") String pipeline,
            @RequestBody String raw) throws IOException {
        PipelineDefinition definition = null;

        try {
            definition = tryParsePipelineDef(raw);
            definition.check();
        } catch (ParseErrorException e) {
            return toJsonResponseEntity(e.getParseError());
        } catch (Throwable t) {
            return ResponseEntity.ok(t.getMessage());
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

        return ResponseEntity.ok(null);
    }

    @PostMapping("pipelines/check")
    public ResponseEntity<String> checkPipelineDef(@RequestBody String raw) throws JsonProcessingException {
        try {
            tryParsePipelineDef(raw);
            return ResponseEntity.ok(null);
        } catch (ParseErrorException e) {
            return toJsonResponseEntity(e.getParseError());
        } catch (Throwable t) {
            return ResponseEntity.ok(t.getMessage());
        }
    }

    @Nonnull
    public static ResponseEntity<String> toJsonResponseEntity(@Nonnull ParseError error) throws JsonProcessingException {
        var response = new ObjectMapper(new JsonFactory()).writeValueAsString(error);
        return ResponseEntity.ok(response);
    }

    @Nonnull
    public static PipelineDefinition tryParsePipelineDef(@Nonnull String raw) throws IOException, ParseErrorException {
        try (var bais = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8))) {
            return ProjectRepository.defaultReader(PipelineDefinition.class).load(bais);
        } catch (JsonMappingException e) {
            if (e.getCause() instanceof MarkedYAMLException) {
                var cause = (MarkedYAMLException) e.getCause();
                var mark  = cause.getProblemMark();
                throw new ParseErrorException(
                        e,
                        new ParseError(
                                mark.getLine(),
                                mark.getColumn(),
                                cause.getMessage()
                        )
                );
            } else if (e instanceof MismatchedInputException) {
                var cause = (MismatchedInputException) e;
                var location = cause.getLocation();
                throw new ParseErrorException(e, new ParseError(
                        location.getLineNr(),
                        location.getColumnNr(),
                        cause.getMessage()
                ));
            }
            throw e;
        } catch (MarkedYAMLException e) {
            throw new ParseErrorException(e, new ParseError(
                    e.getProblemMark().getLine(),
                    e.getProblemMark().getColumn(),
                    e.getMessage()
            ));
        }
    }

    @PostMapping("pipelines/create")
    public Optional<PipelineInfo> createPipeline(@RequestBody String name) {
        var id = PipelineDefinitionRepository.derivePipelineIdFromName(name);
        return this.winslow
                .getPipelineRepository()
                .getPipeline(id)
                .exclusive()
                .flatMap(container -> {
                    try {
                        if (container.get().isEmpty()) {
                            var def = new PipelineDefinition(
                                    name,
                                    "Automatically generated description for '" + name + "'",
                                    new UserInput(
                                            UserInput.Confirmation.Once,
                                            List.of("SOME", "ENV_VARS", "THAT_MUST_BE_SET")
                                    ),
                                    List.of(new StageDefinition(
                                            "Auto Modest Stage",
                                            "Automatically generated stage description",
                                            new Image("library/hello-world", new String[0]),
                                            null,
                                            new UserInput(UserInput.Confirmation.Never, Collections.emptyList()),
                                            Map.of("SOME", "VALUE"),
                                            null,
                                            false,
                                            false,
                                            null,
                                            null,
                                            null,
                                            true
                                    ), new StageDefinition(
                                            "Auto Nvidia Stage",
                                            "Automatically generated stage description",
                                            new Image("nvidia/cuda", new String[]{"nvidia-smi"}),
                                            new Requirements(
                                                    0,
                                                    0,
                                                    new Requirements.Gpu(1, "nvidia", new String[]{"cuda"})
                                            ),
                                            new UserInput(UserInput.Confirmation.Never, Collections.emptyList()),
                                            Map.of("ANOTHER", "VALUE"),
                                            null,
                                            true,
                                            false,
                                            null,
                                            null,
                                            null,
                                            true
                                    ), new StageDefinition(
                                            "Auto Stage 3",
                                            "Downloading more RAM for speedup",
                                            new Image("library/hello-world", new String[]{}),
                                            new Requirements(1, 10240, null),
                                            new UserInput(UserInput.Confirmation.Always, Collections.emptyList()),
                                            Map.of("GIMME", "MOAR RAM"),
                                            null,
                                            null,
                                            false,
                                            null,
                                            null,
                                            null,
                                            true
                                    )),
                                    Map.of("some-key", "some-value", "another-key", "another-value"),
                                    null,
                                    null
                            );
                            container.update(def);
                            return Optional.of(PipelineInfoConverter.from(id, def));
                        } else {
                            return Optional.empty();
                        }
                    } catch (LockException | IOException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    public static class ParseErrorException extends IOException {
        @Nonnull private final ParseError error;

        public ParseErrorException(@Nonnull Throwable cause, @Nonnull ParseError error) {
            super(cause);
            this.error = error;
        }

        @Nonnull
        public ParseError getParseError() {
            return error;
        }
    }
}
