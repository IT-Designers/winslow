package de.itdesigners.winslow.project;

import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class AuthTokenRepository extends BaseRepository {

    public static final String FILE_SUFFIX = ".tokens" + FILE_EXTENSION;

    private static final Logger LOG = Logger.getLogger(AuthTokenRepository.class.getSimpleName());

    public AuthTokenRepository(
            LockBus lockBus,
            WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getProjectsDirectory();
    }

    @Nonnull
    public Handle<AuthTokens> getAuthTokens(String projectId) {
        var path = Path.of(projectId).getFileName() + FILE_SUFFIX;
        return getHandle(getRepositoryDirectory().resolve(path));
    }


    private Handle<AuthTokens> getHandle(@Nonnull Path path) {
        return createHandle(path, AuthTokens.class);
    }
}
