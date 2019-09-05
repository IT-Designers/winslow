package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;

public class Environment {
    private final WorkDirectoryConfiguration configuration;
    private final ResourceManager            resourceManager;

    public Environment(WorkDirectoryConfiguration configuration, ResourceManager resourceManager) {
        this.configuration = configuration;
        this.resourceManager = resourceManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public WorkDirectoryConfiguration getWorkDirectoryConfiguration() {
        return configuration;
    }
}
