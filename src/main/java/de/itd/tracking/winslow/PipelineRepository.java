package de.itd.tracking.winslow;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.fs.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
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

    private Loader<Pipeline> pipelineLoader() {
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

    public Stream<Pipeline> getPipelinesUnsafe() {
        return getAllInDirectoryUnsafe(
                workDirectoryConfiguration.getPipelinesDirectory(),
                Pipeline.class,
                pipelineLoader()
        );
    }

    public Stream<Pipeline> getPipelines() {
        return getAllInDirectory(
                workDirectoryConfiguration.getPipelinesDirectory(),
                Pipeline.class,
                pipelineLoader()
        );
    }

    public Optional<Pipeline> getPipelineUnsafe(String id) {
        var name = Path.of(id + SUFFIX).getFileName();
        return getSingleUnsafe(
                workDirectoryConfiguration.getPipelinesDirectory().resolve(name),
                Pipeline.class,
                pipelineLoader()
        );
    }

    public Optional<Pipeline> getPipeline(String id) {
        var name = Path.of(id + SUFFIX).getFileName();
        return getSingle(
                workDirectoryConfiguration.getPipelinesDirectory().resolve(name),
                Pipeline.class,
                pipelineLoader()
        );
    }
}
