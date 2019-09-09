package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;

public class NomadRepository extends BaseRepository {


    public NomadRepository(LockBus lockBus, WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getPath().resolve("nomad");
    }


    public Handle<NomadPipeline> getNomadPipeline(@Nonnull String projectId) {
        return createHandle(getRepositoryFile(projectId), NomadPipeline.class);
    }
}
