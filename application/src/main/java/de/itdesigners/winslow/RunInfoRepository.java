package de.itdesigners.winslow;

import de.itdesigners.winslow.api.pipeline.StatsInfo;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunInfoRepository extends BaseRepository {

    private static final Logger LOG = Logger.getLogger(RunInfoRepository.class.getSimpleName());

    private static final String PROPERTY_FILE_PROGRESS                   = "progress";
    private static final String PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY = "log-completed-successfully";
    private static final String PROPERTY_FILE_STATS                      = "stats";
    private static final String PROPERTY_FILE_RESULT                     = "result";

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
        return stageDir.resolve(Path.of(property).getFileName());
    }

    @Nonnull
    protected Optional<Path> getPropertyPathIfStageExists(@Nonnull String stageId, @Nonnull String property) {
        return Optional
                .of(getRepositoryFile(stageId))
                .filter(Files::exists)
                .map(p -> p.resolve(Path.of(property)));
    }

    public void removeAllProperties(@Nonnull String stageId) throws IOException {
        Orchestrator.forcePurge(
                workDirectoryConfiguration.getPath(),
                workDirectoryConfiguration.getPath(),
                getRepositoryFile(stageId),
                1
        );
    }

    public void setPropertyNoThrows(
            @Nonnull String stageId,
            @Nonnull String property,
            @Nonnull Iterable<? extends CharSequence> lines) {
        try {
            this.setProperty(stageId, property, lines);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to set property " + stageId + "." + property);
        }
    }

    public void setProperty(
            @Nonnull String stageId,
            @Nonnull String property,
            @Nonnull Iterable<? extends CharSequence> lines) throws IOException {
        Files.write(getPropertyPath(stageId, property), lines);
    }

    public void appendProperty(
            @Nonnull String stageId,
            @Nonnull String property,
            @Nonnull Iterable<? extends CharSequence> lines) throws IOException {
        try {
            Files.write(getPropertyPath(stageId, property), lines, StandardOpenOption.APPEND);
        } catch (NoSuchFileException e) {
            Files.write(getPropertyPath(stageId, property), lines);
        }
    }

    @Nonnull
    public Optional<String> getProperty(@Nonnull String stageId, @Nonnull String property) throws IOException {
        var path = getPropertyPathIfStageExists(stageId, property);
        if (path.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(Files.readString(path.get()));
        }
    }

    @Nonnull
    public Optional<String> getPropertyNoThrows(@Nonnull String stageId, @Nonnull String property) {
        try {
            return getProperty(stageId, property);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to get property " + stageId + "." + property);
            return Optional.empty();
        }
    }

    public void setProgressHint(@Nonnull String stageId, int progress) {
        try {
            setProperty(stageId, PROPERTY_FILE_PROGRESS, Collections.singleton(Integer.toString(progress)));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save progress hint [" + progress + "] for " + stageId, e);
        }
    }

    public void setResult(@Nonnull String stageId, String key, String value) {
        try {
            appendProperty(stageId, PROPERTY_FILE_RESULT, Collections.singleton(key + "=" + value));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save computation result [" + key + "=" + value + "] for " + stageId, e);
        }
    }

    @Nonnull
    public Optional<Integer> getProgressHint(@Nonnull String stageId) {
        try {
            return getProperty(stageId, PROPERTY_FILE_PROGRESS).map(String::trim).map(Integer::parseInt);
        } catch (NumberFormatException | NoSuchFileException | FileNotFoundException e) {
            LOG.log(Level.FINER, "There is no progress for the stage " + stageId, e);
            return Optional.empty();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read progress hint for " + stageId, e);
            return Optional.empty();
        }
    }

    @Nonnull
    public Optional<Map<String, String>> getResult(@Nonnull String stageId) {
        try {
            Optional<String> fileContent = getProperty(stageId, PROPERTY_FILE_RESULT).map(String::trim);
            return fileContent.map(content -> {
                Map<String, String> result = new HashMap<>();
                content.lines().forEach(line -> {
                    String kv[] = line.split("=");
                    result.put(kv[0], kv[1]);
                });
                return result;
            });
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOG.log(Level.FINER, "There is no result for the stage " + stageId, e);
            return Optional.empty();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read result for " + stageId, e);
            return Optional.empty();
        }
    }

    @Nonnull
    public Optional<StatsInfo> getStats(@Nonnull String stageId) {
        return getStatsIfNotOlderThan(stageId, Long.MAX_VALUE);
    }

    @Nonnull
    public Optional<StatsInfo> getStatsIfStillRelevant(@Nonnull String stageId) {
        return getStatsIfNotOlderThan(stageId, 5_000);
    }

    @Nonnull
    public Optional<StatsInfo> getStatsIfNotOlderThan(@Nonnull String stageId, long duration) {
        var path         = getPropertyPathIfStageExists(stageId, PROPERTY_FILE_STATS);
        var file         = path.map(Path::toFile);
        var lastModified = file.map(File::lastModified);
        var timeDiff     = lastModified.map(lm -> System.currentTimeMillis() - lm);

        if (timeDiff.isPresent() && timeDiff.get() < duration) {
            try (var fis = new FileInputStream(file.get())) {
                return Optional.ofNullable(defaultReader(StatsInfo.class).load(fis));
            } catch (FileNotFoundException e) {
                LOG.log(Level.WARNING, "Failed to find promised property file for " + stageId, e);
                return Optional.empty();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to deserialize stats for " + stageId, e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public void setStats(@Nonnull String stageId, @Nonnull StatsInfo stats) {
        try {
            var path = getPropertyPath(stageId, PROPERTY_FILE_STATS);
            AtomicWriteByUsingTempFile.write(path, os -> defaultWriter().store(os, stats));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to write stats for " + stageId, e);
        }
    }

    void setLogRedirectionCompletedSuccessfullyHint(@Nonnull String stageId) {
        try {
            Files.write(getPropertyPath(stageId, PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY), Collections.emptyList());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to set hint for log redirect having completed", e);
        }
    }

    public boolean hasLogRedirectionCompletedSuccessfullyHint(@Nonnull String stageId) {
        try {
            return Files.exists(getPropertyPath(stageId, PROPERTY_FILE_LOG_COMPLETED_SUCCESSFULLY));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to check whether log redirection has completed", e);
            return false;
        }
    }
}
