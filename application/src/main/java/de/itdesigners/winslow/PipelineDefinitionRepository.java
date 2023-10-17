package de.itdesigners.winslow;

import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PipelineDefinitionRepository extends BaseRepository {

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

    @Nonnull
    public Stream<String> getPipelineIds() {
        try (var files = Files.list(workDirectoryConfiguration.getPipelinesDirectory())) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .toList() // stream from a copy, allowing the IO-Stream to be closed
                    .stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    @Nonnull
    public Stream<Handle<PipelineDefinition>> getPipelines() {
        return listAll().map(path -> createHandle(path, PipelineDefinition.class));
    }

    @Nonnull
    public Handle<PipelineDefinition> getPipeline(@Nonnull String id) {
        var name = Path.of(id + FILE_EXTENSION).getFileName();
        return createHandle(workDirectoryConfiguration.getPipelinesDirectory().resolve(name), PipelineDefinition.class);
    }

    @Nonnull
    public Optional<PipelineDefinition> getPipelineDefinitionReadonly(Project project) {
        return getPipeline(project.getPipelineDefinitionId()).unsafe();
    }
}
