package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;

public class Environment {

    @Nonnull private final WorkDirectoryConfiguration configuration;
    @Nonnull private final ResourceManager            resourceManager;

    public Environment(@Nonnull WorkDirectoryConfiguration configuration, @Nonnull ResourceManager resourceManager) {
        this.configuration = configuration;
        this.resourceManager = resourceManager;
    }

    @Nonnull
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Nonnull
    public WorkDirectoryConfiguration getWorkDirectoryConfiguration() {
        return configuration;
    }
}
