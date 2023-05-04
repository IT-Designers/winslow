package de.itdesigners.winslow.web.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.ParseError;
import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.*;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.web.PipelineDefinitionInfoConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@RestController
public class PipelinesController {

    private final Winslow winslow;

    public PipelinesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("pipelines")
    public Stream<PipelineDefinitionInfo> getAllPipelines(@Nullable User user) {
        return winslow
                .getPipelineRepository()
                .getPipelineIds()
                .flatMap(id -> winslow
                        .getPipelineRepository()
                        .getPipeline(id)
                        .unsafe()
                        .stream()
                        .filter(p -> user != null && p.canBeAccessedBy(user))
                        .map(PipelineDefinitionInfoConverter::from));
    }

    @GetMapping("pipelines/{pipeline}")
    public Optional<PipelineDefinitionInfo> getPipeline(
            @Nullable User user,
            @PathVariable("pipeline") String pipelineId) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipelineId)
                .unsafe()
                .filter(pd -> user != null && pd.canBeAccessedBy(user))
                .map(PipelineDefinitionInfoConverter::from);
    }

    @PutMapping("pipelines")
    public ResponseEntity<String> setPipeline(
            @Nullable User user,
            @RequestBody PipelineDefinitionInfo pipeline) throws IOException {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user");
        }

        return storePipelineDefinition(user, PipelineDefinitionInfoConverter.reverse(pipeline));
    }


    @GetMapping("pipelines/{pipeline}/raw")
    public Optional<String> getPipelineRaw(@Nullable User user, @PathVariable("pipeline") String pipelineId) {
        var handle = winslow
                .getPipelineRepository()
                .getPipeline(pipelineId);

        return handle
                .unsafe()
                .filter(pd -> user != null && pd.canBeAccessedBy(user))
                .flatMap(_pd -> handle.unsafeRaw());
    }

    @PutMapping("pipelines/{pipeline}/raw")
    public ResponseEntity<String> setPipeline(
            @Nullable User user,
            @PathVariable("pipeline") String pipelineId,
            @RequestBody String raw) throws IOException {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user");
        }

        PipelineDefinition definition;

        try {
            definition = tryParsePipelineDef(raw);
            definition.check();

            if (!Objects.equals(definition.id(), pipelineId)) {
                throw new IllegalArgumentException("The id of the pipeline does not match the path pipeline-id");
            }

        } catch (ParseErrorException e) {
            return toJsonResponseEntity(e.getParseError());
        } catch (Throwable t) {
            return ResponseEntity.ok(t.getMessage());
        }

        return storePipelineDefinition(user, definition);
    }

    @Nonnull
    private ResponseEntity<String> storePipelineDefinition(
            @Nonnull User user,
            @Nonnull PipelineDefinition definition) throws IOException {
        var exclusive = winslow
                .getPipelineRepository()
                .getPipeline(definition.id())
                .exclusive();

        if (exclusive.isPresent()) {
            var container = exclusive.get();
            var lock      = container.getLock();
            var previous  = container.getNoThrow();

            try (container; lock) {
                if (previous.isPresent()) {
                    if (!previous.get().canBeManagedBy(user)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                    }
                } else {
                    definition = definition.withUserAndRole(user.name(), Role.OWNER);
                }

                container.update(definition);
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("pipelines/check")
    public ResponseEntity<String> checkPipelineDef(@RequestBody String raw) throws JsonProcessingException {
        try {
            tryParsePipelineDef(raw);
            return ResponseEntity.ok(null);
        } catch (ParseErrorException e) {
            return toJsonResponseEntity(e.getParseError());
        } catch (ValueInstantiationException e) {
            return toJsonResponseEntity(e);
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
    public static ResponseEntity<String> toJsonResponseEntity(@Nonnull ValueInstantiationException exception) throws JsonProcessingException {
        return toJsonResponseEntity(new ParseError(
                exception.getLocation().getLineNr(),
                exception.getLocation().getColumnNr(),
                exception.getMessage()
        ));
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
                var cause    = (MismatchedInputException) e;
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
    public Optional<PipelineDefinitionInfo> createPipeline(@Nullable User user, @RequestBody String name) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user");
        }

        var id = UUID.randomUUID().toString();
        return this.winslow
                .getPipelineRepository()
                .getPipeline(id)
                .exclusive()
                .flatMap(container -> {
                    try (container) {
                        if (container.get().isEmpty()) {
                            var id0 = UUID.randomUUID();
                            var id1 = UUID.randomUUID();
                            var id2 = UUID.randomUUID();

                            var def = new PipelineDefinition(
                                    id,
                                    name,
                                    "Automatically generated description for '" + name + "'",
                                    new UserInput(
                                            UserInput.Confirmation.ONCE,
                                            List.of("SOME", "ENV_VARS", "THAT_MUST_BE_SET")
                                    ),
                                    List.of(new StageWorkerDefinition(
                                            id0,
                                            "Sample Modest Stage",
                                            "Automatically generated stage description",
                                            List.of(id1),
                                            new Image("library/hello-world"),
                                            new Requirements(),
                                            new UserInput(),
                                            Map.of("SOME", "VALUE"),
                                            null,
                                            null,
                                            false,
                                            false,
                                            false
                                    ), new StageWorkerDefinition(
                                            id1,
                                            "Sample Nvidia Stage",
                                            "Automatically generated stage that reqires a GPU",
                                            List.of(id2),
                                            new Image(
                                                    "nvidia/cuda:11.8.0-base-ubuntu22.04",
                                                    new String[]{"nvidia-smi"}
                                            ),
                                            new Requirements(
                                                    null,
                                                    null,
                                                    new Requirements.Gpu(1, "nvidia", new String[]{"cuda"}),
                                                    null
                                            ),
                                            new UserInput(UserInput.Confirmation.NEVER, Collections.emptyList()),
                                            Map.of("ANOTHER", "VALUE"),
                                            null,
                                            null,
                                            true,
                                            false,
                                            false
                                    ), new StageWorkerDefinition(
                                            id2,
                                            "Sample Stage 3",
                                            "Another example",
                                            null,
                                            new Image("library/hello-world", new String[]{}),
                                            new Requirements(1, 10240L, null, null),
                                            new UserInput(UserInput.Confirmation.ALWAYS, Collections.emptyList()),
                                            Map.of("GIMME", "MOAR RAM"),
                                            null,
                                            null,
                                            false,
                                            false,
                                            false
                                    )),
                                    Map.of("some-key", "some-value", "another-key", "another-value"),
                                    new DeletionPolicy(),
                                    Collections.emptyList(),
                                    List.of(new Link(
                                            user.name(),
                                            Role.OWNER
                                    )),
                                    false
                            );
                            container.update(def);
                            return Optional.of(PipelineDefinitionInfoConverter.from(def));
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
