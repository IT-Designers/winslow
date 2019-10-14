package de.itd.tracking.winslow;


import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.Pipeline;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PipelineRepository extends BaseRepository {

    private static final String SUFFIX_PIPELINE = ".pipeline.toml";


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
        return listAll(SUFFIX_PIPELINE)
                .filter(path -> path.toString().endsWith(SUFFIX_PIPELINE))
                .map(path -> createHandle(path, Pipeline.class));
    }

    @Nonnull
    Handle<Pipeline> getPipeline(@Nonnull String projectId) {
        return createHandle(getRepositoryFile(projectId, SUFFIX_PIPELINE), Pipeline.class);
    }
}
