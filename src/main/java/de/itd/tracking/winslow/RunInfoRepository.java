package de.itd.tracking.winslow;

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

    private static final Logger LOG                                      = Logger.getLogger(RunInfoRepository.class.getSimpleName());
    private static final String PROPERTY_FILE_PROGRESS                   = "progress";
    private static final String PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY = "log-completed-successfully";

    public RunInfoRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getRunDirectory().resolve("stage");
    }

    @Nonnull
    protected Path getPropertyPath(@Nonnull String stageId, @Nonnull String property) throws IOException {
        var stageDir = getRepositoryFile(stageId);
        Files.createDirectories(stageDir);
        return stageDir.resolve(property);
    }

    @Nonnull
    protected Optional<Path> getPropertyPathIfExists(@Nonnull String stageId, @Nonnull String property) {
        var stageDir = getRepositoryFile(stageId);
        if (Files.exists(stageDir)) {
            return Optional.of(stageDir);
        } else {
            return Optional.empty();
        }
    }

    public void removeAllProperties(@Nonnull String stageId) throws IOException {
        var directory  = getRepositoryFile(stageId);
        var maxRetries = 3;
        for (int i = 0; i < maxRetries && directory.toFile().exists(); ++i) {
            var index = i;
            try (var stream = Files.walk(directory)) {
                stream.forEach(entry -> {
                    try {
                        Files.deleteIfExists(entry);
                    } catch (NoSuchFileException ignored) {
                    } catch (IOException e) {
                        if (index + 1 == maxRetries) {
                            LOG.log(Level.WARNING, "Failed to delete: " + entry, e);
                        }
                    }
                });
            }
        }
        Files.deleteIfExists(directory);
    }


    public void setProgressHint(@Nonnull String stageId, int progress) {
        try {
            Files.write(
                    getPropertyPath(stageId, PROPERTY_FILE_PROGRESS),
                    Collections.singletonList(Integer.toString(progress))
            );
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save progress hint [" + progress + "] for " + stageId, e);
        }
    }

    public Optional<Integer> getProgressHint(@Nonnull String stageId) {
        try {
            var path = getPropertyPathIfExists(stageId, PROPERTY_FILE_PROGRESS);
            if (path.isEmpty()) {
                return Optional.empty();
            } else {
                var content = Files.readString(path.get()).trim();
                return Optional.of(Integer.parseInt(content));
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOG.log(Level.FINER, "There is no progress for the stage " + stageId, e);
            return Optional.empty();
        } catch (NumberFormatException | IOException e) {
            LOG.log(Level.WARNING, "Failed to read progress hint for " + stageId, e);
            return Optional.empty();
        }
    }

    void setLogRedirectionCompletedSuccessfullyHint(@Nonnull String stageId) {
        try {
            Files.write(getPropertyPath(stageId, PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY), Collections.emptyList());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to set hint for log redirect having completed", e);
        }
    }

    boolean hasLogRedirectionCompletedSuccessfullyHint(@Nonnull String stageId) {
        try {
            return Files.exists(getPropertyPath(stageId, PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to check whether log redirection has completed", e);
            return false;
        }
    }
}
