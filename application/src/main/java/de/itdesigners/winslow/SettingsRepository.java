package de.itdesigners.winslow;

import de.itdesigners.winslow.api.settings.UserResourceLimitation;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsRepository extends BaseRepository {

    private static final int                     MAX_LOCK_RETRIES      = 5;
    private static final Logger                  LOG                   = Logger.getLogger(SettingsRepository.class.getSimpleName());
    public static final  Consumer<LockException> LOCK_EXCEPTION_LOGGER = lockException -> LOG.log(
            Level.WARNING,
            "Failed to acquire lock: " + lockException.getMessage(),
            lockException
    );

    @Nullable private Map<String, String> globalEnvironmentVariables = null;


    public SettingsRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);

        this.lockBus.registerEventListener(Event.Command.RELEASE, event -> {
            try {
                var path = workDirectoryConfiguration.getPath().resolve(event.getSubject());
                if (path.getParent().equals(getRepositoryDirectory())) {
                    invalidateCorrespondingValue(path);
                }
            } catch (InvalidPathException e) {
                // well... do not react on this event
            }
        }, LockBus.RegistrationOption.NOTIFY_ONLY_IF_ISSUER_IS_NOT_US);
    }

    private synchronized void invalidateCorrespondingValue(Path path) {
        if (path.equals(getGlobalEnvironmentVariablesPath())) {
            this.globalEnvironmentVariables = null;
            LOG.info("GlobalEnvironmentVariables invalidated");
        }
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getSettingsDirectory();
    }

    private Path getGlobalEnvironmentVariablesPath() {
        return getRepositoryDirectory().resolve("env.properties");
    }

    private Path getUserResourceLimitationPath() {
        return getRepositoryDirectory().resolve("user-resource-limitation.yml");
    }

    @Nonnull
    public Map<String, String> getGlobalEnvironmentVariables() throws IOException {
        return Collections.unmodifiableMap(getOrLoadGlobalEnvironmentVariables());
    }

    @Nonnull
    private Map<String, String> getOrLoadGlobalEnvironmentVariables() throws IOException {
        if (this.globalEnvironmentVariables == null) {
            var lockedContainer = getLocked(
                    getGlobalEnvironmentVariablesPath(),
                    propertiesReader(),
                    propertiesWriter(),
                    MAX_LOCK_RETRIES,
                    LOCK_EXCEPTION_LOGGER
            ).orElseThrow(() -> new IOException("Failed to acquire lock"));

            try (lockedContainer) {
                return this.globalEnvironmentVariables = new TreeMap<>(
                        lockedContainer.get().orElseGet(Collections::emptyMap)
                );
            } catch (LockException e) {
                throw new IOException("Lock expired too early", e);
            }
        } else {
            return this.globalEnvironmentVariables;
        }
    }

    public void updateGlobalEnvironmentVariables(@Nonnull Consumer<Map<String, String>> updater) throws IOException {
        var lockedContainer = getLocked(
                getGlobalEnvironmentVariablesPath(),
                propertiesReader(),
                propertiesWriter(),
                MAX_LOCK_RETRIES,
                LOCK_EXCEPTION_LOGGER
        ).orElseThrow(() -> new IOException("Failed to acquire lock"));

        try (lockedContainer) {
            var variables = new TreeMap<>(lockedContainer.get().orElseGet(Collections::emptyMap));
            updater.accept(variables);
            lockedContainer.update(variables);
            this.globalEnvironmentVariables = new TreeMap<>(variables);
        } catch (LockException e) {
            throw new IOException("Lock expired too early", e);
        }
    }

    @Nonnull
    public Handle<UserResourceLimitation> getUserResourceLimitations() {
        return createHandle(getUserResourceLimitationPath(), UserResourceLimitation.class);
    }

    public void updateUserResourceLimitations(@Nonnull UserResourceLimitation limit) throws IOException {
        try (var container = getUserResourceLimitations().exclusive(MAX_LOCK_RETRIES).orElseThrow(IOException::new)) {
            container.update(limit);
        }
    }

    @Nonnull
    private static Reader<Map<String, String>> propertiesReader() {
        return input -> {
            var result     = new TreeMap<String, String>();
            var properties = new Properties();
            properties.load(input);
            properties.forEach((key, value) -> {
                result.put((String) key, (String) value);
            });
            return result;
        };
    }

    @Nonnull
    private static Writer<Map<String, String>> propertiesWriter() {
        return (outputStream, value) -> {
            var properties = new Properties();
            value.forEach(properties::setProperty);
            properties.store(outputStream, null);
        };
    }
}
