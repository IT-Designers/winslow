package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.LockedOutputStream;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class LogRepository extends BaseRepository {

    private static final int LOCK_DURATION_MS = 20_000;

    public LogRepository(@Nonnull LockBus lockBus, @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getLogsDirectory();
    }

    @Nonnull
    public LockedOutputStream getRawOutputStream(@Nonnull String projectId, @Nonnull String stageId) throws LockException, FileNotFoundException {
        var path = getLogFile(projectId, stageId);
        var lock = getLockForPath(path, LOCK_DURATION_MS);
        return new LockedOutputStream(path.toFile(), lock);
    }

    @Nonnull
    public InputStream getRawInputStreamNonExclusive(@Nonnull String projectId, @Nonnull String stageId) throws FileNotFoundException {
        var path = getLogFile(projectId, stageId);
        return new FileInputStream(path.toFile());
    }

    private Path getLogFile(@Nonnull String projectId, @Nonnull String stageId) {
        return getRepositoryFile(projectId + "." + stageId);
    }
}
