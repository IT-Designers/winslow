package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineDefinitionRepository extends BaseRepository {

    private static final Pattern INVALID_ID_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-]");
    private static final Pattern MULTI_DASH           = Pattern.compile("-[-]+");


    public static final Logger LOG    = Logger.getLogger(PipelineDefinitionRepository.class.getSimpleName());
    public static final String SUFFIX = ".toml";

    public PipelineDefinitionRepository(
            LockBus lockBus,
            WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getPipelinesDirectory();
    }

    @Nonnull
    public Stream<Path> listAll() {
        return super.listAll(SUFFIX);
    }

    public Stream<String> getPipelineIdentifiers() {
        try (var files = Files.list(workDirectoryConfiguration.getPipelinesDirectory())) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(SUFFIX))
                    .map(name -> name.substring(0, name.length() - SUFFIX.length()))
                    .collect(Collectors.toUnmodifiableList())
                    .stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private Reader<PipelineDefinition> pipelineLoader() {
        return inputStream -> {
            var toml = new Toml().read(inputStream);
            var stages = toml.getTables("stage").stream().map(table -> table.to(StageDefinition.class)).collect(
                    Collectors.toList());
            var pipe = toml.getTable("pipeline").to(PipelineDefinition.class);

            return new PipelineDefinition(
                    pipe.getName(),
                    pipe.getDescription().orElse(null),
                    pipe.getRequires().orElse(null),
                    stages
            );
        };
    }

    private Writer<PipelineDefinition> pipelineWriter() {
        return (outputStream, pipeline) -> {
            var toml = new HashMap<String, Object>();
            toml.put("stage", pipeline.getStages());
            toml.put(
                    "pipeline",
                    new PipelineDefinition(
                            pipeline.getName(),
                            pipeline.getDescription().orElse(null),
                            pipeline.getRequires().orElse(null),
                            Collections.emptyList()
                    )
            );

            new TomlWriter().write(toml, outputStream);
        };
    }

    public Stream<Handle<PipelineDefinition>> getPipelines() {
        return listAll().map(path -> createHandle(path, pipelineLoader(), pipelineWriter()));
    }


    public Handle<PipelineDefinition> getPipeline(String id) {
        var name = Path.of(id + SUFFIX).getFileName();
        return createHandle(
                workDirectoryConfiguration.getPipelinesDirectory().resolve(name),
                pipelineLoader(),
                pipelineWriter()
        );
    }

    @Nonnull
    public static String derivePipelineIdFromName(@Nonnull String name) {
        return MULTI_DASH.matcher(INVALID_ID_CHARACTER.matcher(name.toLowerCase()).replaceAll("-")).replaceAll("-");
    }
}
