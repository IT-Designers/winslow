package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineRepository extends BaseRepository {

    public static final Logger LOG = Logger.getLogger(PipelineRepository.class.getSimpleName());
    public static final String SUFFIX = ".toml";

    public PipelineRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);

        var dir = workDirectoryConfiguration.getPipelinesDirectory().toFile();
        if (!dir.isDirectory() || (!dir.exists() && !dir.mkdirs())) {
            throw new IOException("Pipelines directory is not valid: " + dir);
        }
    }

    @Nonnull
    @Override
    public Stream<Path> listAll() {
        return listAllInDirectory(workDirectoryConfiguration.getPipelinesDirectory())
                .filter(path -> path.getFileName().toString().endsWith(SUFFIX));
    }

    public Stream<String> getPipelineIdentifiers() {
        try {
            return Files
                    .list(workDirectoryConfiguration.getPipelinesDirectory())
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(SUFFIX))
                    .map(name -> name.substring(0, name.length() - SUFFIX.length()));
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private Reader<Pipeline> pipelineLoader() {
        return inputStream -> {
            var toml = new Toml().read(inputStream);
            var stages = toml.getTables("stage").stream().map(table -> table.to(Stage.class)).collect(Collectors.toList());
            var pipe = toml.getTable("pipeline").to(Pipeline.class);

            return new Pipeline(
                    pipe.getName(),
                    pipe.getDescription().orElse(null),
                    pipe.getUserInput().orElse(null),
                    stages
            );
        };
    }

    private Writer<Pipeline> pipelineWriter() {
        return (outputStream, pipeline) -> {
            var toml = new HashMap<String, Object>();
            toml.put("stage", pipeline.getStages());
            toml.put("pipeline", new Pipeline(
                    pipeline.getName(),
                    pipeline.getDescription().orElse(null),
                    pipeline.getUserInput().orElse(null),
                    Collections.emptyList()
            ));

            new TomlWriter().write(toml, outputStream);
        };
    }

    public Stream<Handle<Pipeline>> getPipelines() {
        return listAll().map(path -> createHandle(path, pipelineLoader(), pipelineWriter()));
    }


    public Handle<Pipeline> getPipeline(String id) {
        var name = Path.of(id + SUFFIX).getFileName();
        return createHandle(
                workDirectoryConfiguration.getPipelinesDirectory().resolve(name),
                pipelineLoader(),
                pipelineWriter()
        );
    }
}
