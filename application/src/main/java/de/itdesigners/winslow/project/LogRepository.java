package de.itdesigners.winslow.project;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.LockedOutputStream;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

public class LogRepository extends BaseRepository {

    private static final int  LOCK_DURATION_MS        = 60_000;
    public static final  char PROJECT_STAGE_SEPARATOR = '.';

    public LogRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getLogsDirectory();
    }

    public boolean isLocked(@Nonnull String projectId, @Nonnull String stageId) {
        return isLocked(getLogFile(projectId, stageId));
    }

    @Nonnull
    public LockedOutputStream getRawOutputStream(
            @Nonnull String projectId,
            @Nonnull String stageId) throws LockException, FileNotFoundException {
        var path = getLogFile(projectId, stageId);
        var lock = getLockForPath(path, LOCK_DURATION_MS);
        return new LockedOutputStream(path.toFile(), lock);
    }

    public boolean deleteLogsIfExistsNoThrows(@Nonnull String projectId, @Nonnull String stageId) {
        try {
            return deleteLogsIfExists(projectId, stageId);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to delete log file for project " + projectId + " and stage " + stageId, e);
            return false;
        }
    }

    public boolean deleteLogsIfExists(@Nonnull String projectId, @Nonnull String stageId) throws IOException {
        var path = getLogFile(projectId, stageId);
        return Files.deleteIfExists(path);
    }

    public long getLogSize(
            @Nonnull String projectId,
            @Nonnull String stageId) {
        var path = getLogFile(projectId, stageId);
        var file = path.toFile();
        return file.length();
    }

    @Nonnull
    public InputStream getRawInputStreamNonExclusive(
            @Nonnull String projectId,
            @Nonnull String stageId,
            long skipBytesLineAligned) throws IOException {
        var path = getLogFile(projectId, stageId);
        var fis  = new FileInputStream(path.toFile());
        if (skipBytesLineAligned >= 1) {
            try {
                fis.skip(skipBytesLineAligned - 1);
                while (true) {
                    int read = fis.read();
                    if (read < 0 || read == '\n') {
                        break;
                    }
                }
            } catch (IOException e) {
                fis.close();
                throw e;
            }
        }
        return fis;
    }

    private Path getLogFile(@Nonnull String projectId, @Nonnull String stageId) {
        return getRepositoryFile(projectId + PROJECT_STAGE_SEPARATOR + stageId);
    }

    @Nonnull
    public Optional<String> getProjectIdForLogPath(@Nonnull Path path) {
        var name  = path.getFileName().toString();
        var index = name.indexOf(PROJECT_STAGE_SEPARATOR);
        if (index >= 0) {
            return Optional.of(name.substring(0, index));
        } else {
            return Optional.empty();
        }
    }
}
