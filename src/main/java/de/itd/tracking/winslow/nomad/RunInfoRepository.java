package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.BaseRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunInfoRepository extends BaseRepository {

    private static final Logger LOG                  = Logger.getLogger(RunInfoRepository.class.getSimpleName());
    private static final String SUFFIX_PROGRESS      = ".progress";
    private static final String SUFFIX_LOG_COMPLETED = ".log-completed-successfully";

    public RunInfoRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getRunDirectory().resolve("nomad");
    }

    boolean setProgressHint(@Nonnull String projectId, int progress) {
        try {
            Files.write(
                    getRepositoryFile(projectId, SUFFIX_PROGRESS),
                    Collections.singletonList(Integer.toString(progress))
            );
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save progress hint [" + progress + "] for " + projectId, e);
            return false;
        }

    }

    Optional<Integer> getProgressHint(@Nonnull String projectId) {
        try {
            return Optional.of(Integer.parseInt(Files
                                                        .readString(getRepositoryFile(projectId, SUFFIX_PROGRESS))
                                                        .trim()));
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOG.log(Level.FINER, "There is no progress for the project " + projectId, e);
            return Optional.empty();
        } catch (NumberFormatException | IOException e) {
            LOG.log(Level.WARNING, "Failed to read progress hint for " + projectId, e);
            return Optional.empty();
        }
    }

    void setLogRedirectionCompletedSuccessfullyHint(@Nonnull String projectId, @Nonnull String stageId) {
        try {
            Files.write(getLogRedirectionCompletedSuccessfullyPath(projectId, stageId), Collections.emptyList());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to set hint for log redirect having completed", e);
        }
    }

    boolean hasLogRedirectionCompletedSuccessfullyHint(@Nonnull String projectId, @Nonnull String stageId) {
        return Files.exists(getLogRedirectionCompletedSuccessfullyPath(projectId, stageId));
    }

    private Path getLogRedirectionCompletedSuccessfullyPath(@Nonnull String projectId, @Nonnull String stageId) {
        return getRepositoryFile(projectId + "." + stageId, SUFFIX_LOG_COMPLETED);
    }
}
