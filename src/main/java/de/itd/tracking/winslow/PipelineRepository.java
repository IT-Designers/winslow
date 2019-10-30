package de.itd.tracking.winslow;


import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.pipeline.Pipeline;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PipelineRepository extends BaseRepository {

    public static final String FILE_SUFFIX = ".pipeline.toml";


    public PipelineRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getPath().resolve("projects");
    }

    @Nonnull
    Stream<Handle<Pipeline>> getAllPipelines() {
        return listAll(FILE_SUFFIX)
                .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                .map(path -> createHandle(path, Pipeline.class));
    }

    @Nonnull
    Handle<Pipeline> getPipeline(@Nonnull String projectId) {
        return createHandle(getRepositoryFile(projectId, FILE_SUFFIX), Pipeline.class);
    }
}
