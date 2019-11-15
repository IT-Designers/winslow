package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineDefinitionRepository extends BaseRepository {

    private static final Pattern INVALID_ID_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-]");
    private static final Pattern MULTI_DASH           = Pattern.compile("-[-]+");


    public static final Logger LOG = Logger.getLogger(PipelineDefinitionRepository.class.getSimpleName());

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
        return super.listAll(FILE_EXTENSION);
    }

    public Stream<String> getPipelineIdentifiers() {
        try (var files = Files.list(workDirectoryConfiguration.getPipelinesDirectory())) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .collect(Collectors.toUnmodifiableList())
                    .stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    public Stream<Handle<PipelineDefinition>> getPipelines() {
        return listAll().map(path -> createHandle(path, PipelineDefinition.class));
    }


    public Handle<PipelineDefinition> getPipeline(String id) {
        var name = Path.of(id + FILE_EXTENSION).getFileName();
        return createHandle(workDirectoryConfiguration.getPipelinesDirectory().resolve(name), PipelineDefinition.class);
    }

    @Nonnull
    public static String derivePipelineIdFromName(@Nonnull String name) {
        return MULTI_DASH.matcher(INVALID_ID_CHARACTER.matcher(name.toLowerCase()).replaceAll("-")).replaceAll("-");
    }
}
